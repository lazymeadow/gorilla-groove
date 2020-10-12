package com.example.groove.services

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.properties.FileStorageProperties
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.logger
import com.example.groove.util.withNewExtension
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

@Service
@ConditionalOnProperty(name = ["aws.store.in.s3"], havingValue = "false")
class SystemStorageService(
		private val fileStorageProperties: FileStorageProperties,
		trackRepository: TrackRepository,
		trackLinkRepository: TrackLinkRepository
) : FileStorageService(trackRepository, trackLinkRepository, fileStorageProperties) {

	override fun loadSong(track: Track, audioFormat: AudioFormat): File {
		val fileName = when (audioFormat) {
			AudioFormat.OGG -> track.fileName
			AudioFormat.MP3 -> track.fileName.withNewExtension(AudioFormat.MP3.extension)
		}
		val sourceFile = File(fileStorageProperties.musicDirectoryLocation + fileName)

		val destinationPath = generateTmpFilePath()

		if (!sourceFile.exists()) {
			throw IllegalStateException("Source file for track with ID: ${track.id} does not exist")
		}
		Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING)

		return destinationPath.toFile()
	}

	override fun storeSong(song: File, trackId: Long, audioFormat: AudioFormat) {
		val fileName = trackId.toString() + audioFormat.extension
		val destinationFile = File(fileStorageProperties.musicDirectoryLocation + fileName)

		// The parent directory might not be made. Make it if it doesn't exist
		destinationFile.parentFile.mkdirs()

		song.copyTo(destinationFile, true)
	}

	override fun storeAlbumArt(albumArt: File, trackId: Long, artSize: ArtSize) {
		val parentDirectoryName = trackId / 1000 // Only put 1000 album art in a single directory for speed
		val fileName = when (artSize) {
			ArtSize.LARGE -> "$trackId.png"
			ArtSize.SMALL -> "$trackId-64x64.png"
		}
		val destinationFile = File("${fileStorageProperties.albumArtDirectoryLocation}$parentDirectoryName/$fileName")

		// The parent directory might not be made. Make it if it doesn't exist
		destinationFile.parentFile.mkdirs()

		albumArt.copyTo(destinationFile, true)
	}

	override fun loadAlbumArt(trackId: Long, artSize: ArtSize): File? {
		val parentDirectoryName = trackId / 1000 // Only put 1000 album art in a single directory for speed
		val sourceFile = File("${fileStorageProperties.albumArtDirectoryLocation}$parentDirectoryName/$trackId${artSize.systemFileExtension}")

		if (!sourceFile.exists()) {
			return null
		}

		val destinationPath = generateTmpFilePath()

		Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING)

		return destinationPath.toFile()
	}

	override fun copyAllAlbumArt(trackSourceId: Long, trackDestinationId: Long) {
		val parentDirectory = trackSourceId / 1000
		val destinationDirectory = trackDestinationId / 1000

		ArtSize.values().forEach { artSize ->
			logger.info("Copying album art for size $artSize and source track ID: $trackSourceId and destination ID: $trackDestinationId")

			val sourceFile = File("${fileStorageProperties.albumArtDirectoryLocation}$parentDirectory/$trackSourceId${artSize.systemFileExtension}")

			if (!sourceFile.exists()) {
				return
			}

			val destinationFile = File("${fileStorageProperties.albumArtDirectoryLocation}$destinationDirectory/$trackDestinationId${artSize.systemFileExtension}")

			sourceFile.copyTo(destinationFile, true)
		}
	}

	override fun deleteSong(fileName: String) {
		AudioFormat.values().forEach {
			val nameWithExtension = fileName.withNewExtension(it.extension)

			val success = File(fileStorageProperties.musicDirectoryLocation + nameWithExtension).delete()
			if (!success) {
				logger.error("The file $nameWithExtension should have been deleted, but couldn't be")
			}
		}
	}

	override fun getSongLink(trackId: Long, anonymousAccess: Boolean, audioFormat: AudioFormat): String {
		return getCachedTrackLink(trackId, anonymousAccess, audioFormat, false) { track ->
			val fileName = when (audioFormat) {
				AudioFormat.OGG -> track.fileName
				AudioFormat.MP3 -> track.fileName.withNewExtension(AudioFormat.MP3.extension)
			}

			// The random key makes it possible to cache-bust the frontend while testing link invalidation locally
			val randomKey = UUID.randomUUID()
			// This is pretty jank. But it is only intended for use in local development right now
			"http://localhost:8080/music/$fileName?key=$randomKey"
		}
	}

	override fun getAlbumArtLink(trackId: Long, anonymousAccess: Boolean, artSize: ArtSize): String? {
		return getCachedTrackLink(trackId, anonymousAccess, isArtLink =  true, artSize = artSize) {
			val parentDir = trackId / 1000
			val randomKey = UUID.randomUUID()
			"http://localhost:8080/album-art/$parentDir/$trackId${artSize.systemFileExtension}?key=$randomKey"
		}
	}

	override fun copySong(sourceFileName: String, destinationFileName: String, audioFormat: AudioFormat) {
		val sourceFile = File(fileStorageProperties.musicDirectoryLocation + sourceFileName)
		val destFile = File(fileStorageProperties.musicDirectoryLocation + destinationFileName)

		sourceFile.copyTo(destFile, true)
	}

	companion object {
		val logger = logger()
	}
}
