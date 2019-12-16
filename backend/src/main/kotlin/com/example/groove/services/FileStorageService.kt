package com.example.groove.services

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.db.model.TrackLink
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.properties.FileStorageProperties
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Timestamp
import java.util.*

abstract class FileStorageService(
		private val trackRepository: TrackRepository,
		private val trackLinkRepository: TrackLinkRepository,
		private val fileStorageProperties: FileStorageProperties
) {
	abstract fun storeSong(song: File, trackId: Long)
	abstract fun loadSong(track: Track): File
	abstract fun storeAlbumArt(albumArt: File, trackId: Long)
	abstract fun loadAlbumArt(trackId: Long): File?
	abstract fun copyAlbumArt(trackSourceId: Long, trackDestinationId: Long)

	abstract fun getSongLink(trackId: Long, anonymousAccess: Boolean): String
	abstract fun getAlbumArtLink(trackId: Long, anonymousAccess: Boolean): String?
	abstract fun deleteSong(fileName: String)

	// Do all of this in a synchronized, new transaction to prevent race conditions with link creation / searching
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	fun getCachedSongLink(trackId: Long, anonymousAccess: Boolean, newLinkFun: (track: Track) -> String): String {
		val track = loadAuthenticatedTrack(trackId, anonymousAccess)

		synchronized(this) {
			val trackLink = trackLinkRepository.findUnexpiredByTrackId(track.id)

			return when {
				trackLink != null -> trackLink.link
				!anonymousAccess -> cacheSongLink(track, newLinkFun(track))
				else -> throw ResourceNotFoundException("No valid link found")
			}
		}
	}

	private fun cacheSongLink(track: Track, link: String): String {
		val expiration = Timestamp(expireHoursOut(4).time)
		val trackLink = TrackLink(track = track, link = link, expiresAt = expiration)
		trackLinkRepository.save(trackLink)

		return link
	}

	fun getTrackForAlbumArt(trackId: Long, anonymousAccess: Boolean): Track {
		val track = loadAuthenticatedTrack(trackId, anonymousAccess)

		// If we are doing anonymous access, also make sure that the track is within its temporary access time
		if  (anonymousAccess) {
			trackLinkRepository.findUnexpiredByTrackId(track.id)
					?: throw IllegalArgumentException("Album art for track ID: $trackId not available to anonymous users!")
		}

		return track
	}

	private fun loadAuthenticatedTrack(trackId: Long, anonymousAccess: Boolean): Track {
		val track = trackRepository.findById(trackId).unwrap() ?: throw IllegalArgumentException("No track with ID $trackId found")

		if (anonymousAccess) {
			if (track.private) {
				throw IllegalArgumentException("Insufficient privileges to access trackId $trackId")
			}
		} else {
			val user = loadLoggedInUser()
			if (track.private && user.id != track.user.id) {
				throw IllegalArgumentException("Insufficient privileges to access trackId $trackId")
			}
		}

		return track
	}

	protected fun expireHoursOut(hours: Int): Date {
		val calendar = Calendar.getInstance()
		calendar.add(Calendar.HOUR, hours)

		return calendar.time
	}

	protected fun generateTmpFilePath(): Path {
		val tmpFileName = UUID.randomUUID().toString() + ".ogg"
		return Paths.get(fileStorageProperties.tmpDir + tmpFileName)
	}
}
