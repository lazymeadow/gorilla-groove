package com.example.groove.services

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.properties.MusicProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnProperty(name = ["aws.store.in.s3"], havingValue = "false")
class SystemStorageService(
		private val musicProperties: MusicProperties,
		trackRepository: TrackRepository,
		trackLinkRepository: TrackLinkRepository
) : FileStorageService(trackRepository, trackLinkRepository) {

	override fun storeSong(song: File, trackId: Long) {
		val destinationFile = File("${musicProperties.musicDirectoryLocation}$trackId.ogg")

		// The parent directory might not be made. Make it if it doesn't exist
		destinationFile.parentFile.mkdirs()

		song.copyTo(destinationFile, true)
	}

	override fun storeAlbumArt(albumArt: File, trackId: Long) {
		val parentDirectoryName = trackId / 1000 // Only put 1000 album art in a single directory for speed
		val destinationFile = File("${musicProperties.albumArtDirectoryLocation}$parentDirectoryName/$trackId.png")

		// The parent directory might not be made. Make it if it doesn't exist
		destinationFile.parentFile.mkdirs()

		albumArt.copyTo(destinationFile, true)
	}

	override fun copyAlbumArt(trackSourceId: Long, trackDestinationId: Long) {
		val parentDirectory = trackSourceId / 1000
		val sourceFile = File("${musicProperties.albumArtDirectoryLocation}$parentDirectory/$trackSourceId.png")

		if (!sourceFile.exists()) {
			return
		}

		val destinationDirectory = trackDestinationId / 1000
		val destinationFile = File("${musicProperties.albumArtDirectoryLocation}$destinationDirectory/$trackDestinationId.png")

		sourceFile.copyTo(destinationFile, true)
	}

	override fun deleteSong(fileName: String) {
		val success = File(musicProperties.musicDirectoryLocation + fileName).delete()
		if (!success) {
			logger.error("The file $fileName should have been deleted, but couldn't be")
		}
	}

	override fun getSongLink(trackId: Long, anonymousAccess: Boolean): String {
		return getCachedSongLink(trackId, anonymousAccess) { track ->
			// This is pretty jank. But it is only intended for use in local development right now
			"http://localhost:8080/music/${track.fileName}"
		}
	}

	override fun getAlbumArtLink(trackId: Long, anonymousAccess: Boolean): String? {
		getTrackForAlbumArt(trackId, anonymousAccess)

		val parentDir = trackId / 1000
		return "http://localhost:8080/album-art/$parentDir/$trackId.png"
	}

	companion object {
		val logger = LoggerFactory.getLogger(SystemStorageService::class.java)!!
	}
}
