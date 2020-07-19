package com.example.groove.services

import com.example.groove.db.dao.DeviceRepository
import com.example.groove.db.dao.TrackHistoryRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.*
import com.example.groove.dto.UpdateTrackDTO
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.example.groove.util.unwrap

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp


@Service
class TrackService(
		private val trackRepository: TrackRepository,
		private val trackHistoryRepository: TrackHistoryRepository,
		private val deviceRepository: DeviceRepository,
		private val fileStorageService: FileStorageService,
		private val songIngestionService: SongIngestionService
) {

	@Transactional(readOnly = true)
	fun getTracks(
			name: String?,
			artist: String?,
			album: String?,
			userId: Long?,
			searchTerm: String?,
			showHidden: Boolean,
			pageable: Pageable
	): Page<Track> {
		val loggedInId = loadLoggedInUser().id
		val idToLoad = userId ?: loggedInId
		val loadPrivate = loggedInId == idToLoad

		// The clients have an old name for the "addedToLibrary" key and
		// we need to convert it if they are using it to sort
		val newSort = pageable.sort.toString()
				.split(",")
				.map { sortKeyDir ->
					val (key, dir) = sortKeyDir.split(": ")
					val convertedKey = if (key == "createdAt") "addedToLibrary" else key
					Sort.Order(Sort.Direction.fromString(dir), convertedKey)
				}.toMutableList()

		val page = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(newSort))
		return trackRepository.getTracks(
				name = name,
				artist = artist,
				album = album,
				userId = idToLoad,
				loadPrivate = loadPrivate,
				loadHidden = showHidden,
				searchTerm = searchTerm,
				pageable = page
		)
	}

	@Transactional(readOnly = true)
	fun getTracksByIds(ids: Set<Long>): List<Track> {
		val user = loadLoggedInUser()

		val tracks = trackRepository.findAllById(ids).toList()

		// Make sure we found a track for every ID that was requested
		if (tracks.size != ids.size) {
			val foundIds = tracks.map { it.id }.toSet()
			throw ResourceNotFoundException("Could not find tracks with IDs ${ids - foundIds}!")
		}

		// As always, make sure private tracks are only accessible to the user that owns them
		val invalidTracks = tracks.filter { track ->
			track.private && track.user.id != user.id
		}
		if (invalidTracks.isNotEmpty()) {
			val invalidIds = invalidTracks.map { it.id }.toSet()
			throw ResourceNotFoundException("Could not find tracks with IDs $invalidIds!")
		}

		return tracks
	}

	@Transactional(readOnly = true)
	fun getAllTrackCountSinceTimestamp(timestamp: Timestamp): Int {
		return trackRepository.countAllTracksAddedSinceTimestamp(timestamp)
	}

	@Transactional
	fun markSongListenedTo(trackId: Long, deviceId: String, remoteIp: String?) {
		val track = trackRepository.findById(trackId).unwrap()
		val user = loadLoggedInUser()

		if (track == null || track.user.id != user.id) {
			throw IllegalArgumentException("No track found by ID $trackId!")
		}

		// May want to do some sanity checks / server side validation here to prevent this incrementing too often.
		// We know the last played date of a track and can see if it's even possible to have listened to this song
		track.playCount++
		track.lastPlayed = now()
		track.updatedAt = now()

		val savedDevice = deviceRepository.findByDeviceIdAndUser(deviceId, user)
				?: throw IllegalArgumentException("No device found with ID $deviceId for user ${user.name} when saving track history!")

		// Device we used might have been merged into another device. If it was, use the parent device
		val device = savedDevice.mergedDevice ?: savedDevice

		val trackHistory = TrackHistory(track = track, device = device, ipAddress = remoteIp, listenedInReview = track.inReview)
		trackHistoryRepository.save(trackHistory)
	}

	@Transactional
	fun updateTracks(updatingUser: User, updateTrackDTO: UpdateTrackDTO, albumArt: MultipartFile?) {
		updateTrackDTO.trackIds.forEach { trackId ->
			val track = trackRepository.findById(trackId).unwrap()
			val user = loadLoggedInUser()

			if (track == null || track.user.id != user.id) {
				throw IllegalArgumentException("No track found by ID $trackId!")
			}

			updateTrackDTO.name?.let { track.name = it.trim() }
			updateTrackDTO.artist?.let { track.artist = it.trim() }
			updateTrackDTO.featuring?.let { track.featuring = it.trim() }
			updateTrackDTO.album?.let { track.album = it.trim() }
			updateTrackDTO.releaseYear?.let { track.releaseYear = it }
			updateTrackDTO.trackNumber?.let { track.trackNumber = it }
			updateTrackDTO.note?.let { track.note = it.trim() }
			updateTrackDTO.genre?.let { track.genre = it.trim() }
			updateTrackDTO.hidden?.let { track.hidden = it }
			track.updatedAt = now()

			if (albumArt != null) {
				songIngestionService.storeAlbumArtForTrack(albumArt, track, updateTrackDTO.cropArtToSquare)
			} else if (updateTrackDTO.cropArtToSquare) {
				logger.info("User ${user.name} is cropping existing art to a square for track $trackId")
				val art = fileStorageService.loadAlbumArt(trackId)
				if (art == null) {
					logger.info("$trackId does not have album art to crop!")
				} else {
					songIngestionService.storeAlbumArtForTrack(art, track, true)
					art.delete()
				}
			}
		}
	}

	@Transactional
	fun setPrivate(trackIds: List<Long>, private: Boolean) {
		val user = loadLoggedInUser()

		// The DB query is written such that it protects the tracks anyway. So this check is kind
		// of unnecessary. But it allows us to fail the request for the frontend
		trackRepository.findAllById(trackIds).forEach { track ->
			if (track.user.id != user.id) {
				throw IllegalArgumentException("No track with ID: ${track.id} found")
			}
		}
		trackRepository.setPrivateForUser(trackIds, loadLoggedInUser().id, private)
	}

	@Transactional
	fun deleteTracks(loadLoggedInUser: User, trackIds: List<Long>) {
		val tracks = trackRepository.findAllById(trackIds)

		// Check all tracks for permissions FIRST. Otherwise, we might delete a track from disk,
		// then throw an exception and roll back the DB, but the disk deletion would not be undone
		tracks.forEach { track ->
			if (track.user.id != loadLoggedInUser.id) {
				throw IllegalArgumentException("Track with ID: ${track.id} not found")
			}
		}

		tracks.forEach { track ->
			track.deleted = true
			track.updatedAt = now()
			trackRepository.save(track)

			deleteFileIfUnused(track.fileName)
		}
	}

	private fun deleteFileIfUnused(fileName: String) {
		if (trackRepository.findAllByFileName(fileName).isNotEmpty()) {
			logger.info("The track $fileName was being deleted, but another user has this track. Skipping file delete")
			return
		}

		logger.info("Deleting song with name $fileName")
		fileStorageService.deleteSong(fileName)
	}

	@Transactional
	fun importTrack(trackIds: List<Long>): List<Track> {
		val user = loadLoggedInUser()

		val tracksToImport = trackIds.map {
			trackRepository.findById(it).unwrap()
		}
		val invalidTracks = tracksToImport.filter { track ->
			track == null || track.private || track.user.id == user.id
		}
		if (invalidTracks.isNotEmpty()) {
			throw IllegalArgumentException("Invalid track import request. Supplied IDs: $trackIds")
		}

		return tracksToImport.map { track ->
			val now = now()
			val forkedTrack = track!!.copy(
					id = 0,
					user = user,
					createdAt = now,
					updatedAt = now,
					addedToLibrary = now,
					playCount = 0,
					lastPlayed = null,
					hidden = false,
					originalTrack = track
			)

			trackRepository.save(forkedTrack)

			fileStorageService.copyAlbumArt(track.id, forkedTrack.id)

			forkedTrack
		}
	}

	fun getPublicTrackInfo(trackId: Long): Map<String, Any?> {
		// This will throw an exception if anonymous access is not allowed for this file
		val trackLink = fileStorageService.getSongLink(trackId, true, AudioFormat.OGG)

		val albumLink = fileStorageService.getAlbumArtLink(trackId, true)

		val track = trackRepository.findById(trackId).unwrap()!!

		return mapOf(
				"trackLink" to trackLink,
				"albumArtLink" to albumLink,
				"name" to track.name,
				"artist" to track.artist,
				"album" to track.album,
				"releaseYear" to track.releaseYear,
				"length" to track.length
		)
	}

	fun trimTrack(trackId: Long, startTime: String?, duration: String?): Int {
		val track = trackRepository.findById(trackId).unwrap()

		if (track == null || track.user.id != loadLoggedInUser().id) {
			throw IllegalArgumentException("No track found by ID $trackId!")
		}

		val newLength = songIngestionService.trimSong(track, startTime, duration)

		track.length = newLength
		track.updatedAt = now()
		trackRepository.save(track)

		return newLength
	}

	// This track is being given to someone for review. Copy the track with the target user as
	// the new owner. Save it, and copy the album art
	fun saveTrackForUserReview(
			user: User,
			track: Track,
			reviewSource: ReviewSource,
			setAsCopied: Boolean = false
	): Track {
		return track.copy(
				id = 0,
				user = user,
				reviewSource = reviewSource,
				lastReviewed = now(),
				inReview = true,
				private = false,
				hidden = false,
				addedToLibrary = null,
				createdAt = now(),
				playCount = 0,
				lastPlayed = null,
				originalTrack = if (setAsCopied) track else null
		).also { trackCopy ->
			trackRepository.save(trackCopy)
			fileStorageService.copyAlbumArt(track.id, trackCopy.id)
		}
	}

	companion object {
		val logger = logger()
	}
}
