package com.example.groove.services

import com.example.groove.db.dao.TrackHistoryRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.db.model.TrackHistory
import com.example.groove.db.model.User
import com.example.groove.dto.UpdateTrackDTO
import com.example.groove.properties.FFmpegProperties
import com.example.groove.properties.MusicProperties
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.slf4j.LoggerFactory

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.sql.Timestamp
import java.util.*


@Service
class TrackService(
		private val trackRepository: TrackRepository,
		private val trackHistoryRepository: TrackHistoryRepository,
		private val songIngestionService: SongIngestionService,
		private val musicProperties: MusicProperties
) {

	@Transactional(readOnly = true)
	fun getTracks(
			name: String?,
			artist: String?,
			album: String?,
			userId: Long?,
			searchTerm: String?,
			pageable: Pageable
	): Page<Track> {
		val loggedInId = loadLoggedInUser().id
		val idToLoad = userId ?: loggedInId
		val loadHidden = loggedInId == idToLoad

		return trackRepository.getTracks(name, artist, album, idToLoad, loadHidden, searchTerm, pageable)
	}

	@Transactional
	fun markSongListenedTo(trackId: Long) {
		val track = trackRepository.findById(trackId).unwrap()

		if (track == null || track.user != loadLoggedInUser()) {
			throw IllegalArgumentException("No track found by ID $trackId!")
		}

		// May want to do some sanity checks / server side validation here to prevent this incrementing too often.
		// We know the last played date of a track and can see if it's even possible to have listened to this song
		track.playCount++
		track.lastPlayed = Timestamp(Date().time)

		val trackHistory = TrackHistory(track = track)
		trackHistoryRepository.save(trackHistory)
	}

	@Transactional
	fun updateTrack(updatingUser: User, updateTrackDTO: UpdateTrackDTO) {
		val track = trackRepository.findById(updateTrackDTO.trackId).unwrap()

		if (track == null || track.user != loadLoggedInUser()) {
			throw IllegalArgumentException("No track found by ID ${updateTrackDTO.trackId}!")
		}

		updateTrackDTO.name?.let { track.name = it }
		updateTrackDTO.artist?.let { track.artist = it }
		updateTrackDTO.album?.let { track.album = it }
		updateTrackDTO.releaseYear?.let { track.releaseYear = it }
		updateTrackDTO.trackNumber?.let { track.trackNumber = it }
		updateTrackDTO.note?.let { track.note = it }
		updateTrackDTO.genre?.let { track.genre = it }
	}

	@Transactional
	fun setHidden(trackIds: List<Long>, hidden: Boolean) {
		val user = loadLoggedInUser()

		// The DB query is written such that it protects the tracks anyway. So this check is kind
		// of unnecessary. But it allows us to fail the request for the frontend
		trackRepository.findAllById(trackIds).forEach { track ->
			if (track.user.id != user.id) {
				throw IllegalArgumentException("No track with ID: ${track.id} found")
			}
		}
		trackRepository.setHiddenForUser(trackIds, loadLoggedInUser().id, hidden)
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
			trackRepository.save(track)

			deleteFileIfUnused(track.fileName)
		}
	}

	private fun deleteFileIfUnused(fileName: String) {
		if (trackRepository.findAllByFileName(fileName).isNotEmpty()) {
			logger.info("The track $fileName was being deleted, but another user has this track. Skipping file delete")
			return
		}

		val success = File(musicProperties.musicDirectoryLocation + fileName).delete()
		if (!success) {
			logger.error("The file $fileName should have been deleted, but couldn't be")
		}
	}

	@Transactional
	fun importTrack(trackIds: List<Long>): List<Track> {
		val user = loadLoggedInUser()

		val tracksToImport = trackIds.map {
			trackRepository.findById(it).unwrap()
		}
		val invalidTracks = tracksToImport.filter { track ->
			track == null || track.hidden || track.user.id == user.id
		}
		if (invalidTracks.isNotEmpty()) {
			throw IllegalArgumentException("Invalid track import request. Supplied IDs: $trackIds")
		}

		return tracksToImport.map { track ->
			val forkedTrack = track!!.copy(
					id = 0,
					user = user,
					createdAt = Timestamp(Date().time),
					playCount = 0,
					lastPlayed = null
			)

			trackRepository.save(forkedTrack)
			songIngestionService.copyAlbumArt(track.id, forkedTrack.id)

			forkedTrack
		}
	}

	companion object {
		val logger = LoggerFactory.getLogger(TrackService::class.java)!!
	}
}
