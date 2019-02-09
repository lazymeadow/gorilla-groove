package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import java.io.File

abstract class FileStorageService(private val trackRepository: TrackRepository) {
	abstract fun storeSong(song: File, trackId: Long)
	abstract fun storeAlbumArt(albumArt: File, trackId: Long)
	abstract fun copyAlbumArt(trackSourceId: Long, trackDestinationId: Long)

	abstract fun getSongLink(trackId: Long): String
	abstract fun getAlbumArtLink(trackId: Long): String?
	abstract fun deleteSong(fileName: String)

	fun loadAuthenticatedTrack(trackId: Long): Track {
		val track = trackRepository.findById(trackId).unwrap() ?: throw IllegalArgumentException("No track with ID $trackId found")
		val user = loadLoggedInUser()

		if (track.hidden && user.id != track.user.id) {
			throw IllegalArgumentException("Insufficient privileges to access trackId $trackId")
		}

		return track
	}
}
