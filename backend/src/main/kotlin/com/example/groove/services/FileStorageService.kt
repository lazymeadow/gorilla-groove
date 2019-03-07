package com.example.groove.services

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.db.model.TrackLink
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import java.io.File
import java.sql.Timestamp
import java.util.*

abstract class FileStorageService(
		private val trackRepository: TrackRepository,
		private val trackLinkRepository: TrackLinkRepository
) {
	abstract fun storeSong(song: File, trackId: Long)
	abstract fun storeAlbumArt(albumArt: File, trackId: Long)
	abstract fun copyAlbumArt(trackSourceId: Long, trackDestinationId: Long)

	abstract fun getSongLink(trackId: Long, anonymousAccess: Boolean): String
	abstract fun getAlbumArtLink(trackId: Long): String?
	abstract fun deleteSong(fileName: String)

	fun getCachedSongLink(trackId: Long, anonymousAccess: Boolean, newLinkFun: (track: Track) -> String): String {
		val track = loadAuthenticatedTrack(trackId, anonymousAccess)

		val trackLink = trackLinkRepository.findUnexpiredByTrackId(track.id)

		return when {
			trackLink != null -> trackLink.link
			!anonymousAccess -> cacheSongLink(track, newLinkFun(track))
			else -> throw ResourceNotFoundException("No valid link found")
		}
	}

	private fun cacheSongLink(track: Track, link: String): String {
		val expiration = Timestamp(expireHoursOut(4).time)
		val trackLink = TrackLink(track = track, link = link, expiresAt = expiration)
		trackLinkRepository.save(trackLink)

		return link
	}

	fun loadAuthenticatedTrack(trackId: Long, anonymousAccess: Boolean): Track {
		val track = trackRepository.findById(trackId).unwrap() ?: throw IllegalArgumentException("No track with ID $trackId found")

		if (anonymousAccess) {
			if (track.hidden) {
				throw IllegalArgumentException("Insufficient privileges to access trackId $trackId")
			}
		} else {
			val user = loadLoggedInUser()
			if (track.hidden && user.id != track.user.id) {
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
}
