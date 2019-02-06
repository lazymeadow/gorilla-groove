package com.example.groove.services

import com.example.groove.properties.MusicProperties
import org.springframework.stereotype.Service
import java.io.File

@Service
class SystemStorageService(
		private val musicProperties: MusicProperties
) : FileStorageService {

	override fun storeSong(song: File, trackId: Long) {
		val destinationFile = File("${musicProperties.musicDirectoryLocation}$trackId.ogg")

		// The parent directory might not be made. Make it if it doesn't exist
		destinationFile.parentFile.mkdirs()

		song.copyTo(destinationFile)
	}

	override fun storeAlbumArt(tmpAlbumArt: File, trackId: Long) {
		val parentDirectoryName = trackId / 1000 // Only put 1000 album art in a single directory for speed
		val destinationFile = File("${musicProperties.albumArtDirectoryLocation}$parentDirectoryName/$trackId.png")

		// The parent directory might not be made. Make it if it doesn't exist
		destinationFile.parentFile.mkdirs()

		tmpAlbumArt.renameTo(destinationFile)
	}

	override fun getSongLink(trackId: Long): String {
		// This is pretty jank. But it is only intended for use in local development right now
		return "http://localhost:8080/music/$trackId.ogg"
	}

	override fun getAlbumArtLink(trackId: Long): String? {
		val parentDir = trackId / 1000
		return "http://localhost:8080/album-art/$parentDir/$trackId.png"
	}

}