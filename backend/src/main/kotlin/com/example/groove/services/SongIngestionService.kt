package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.User
import com.example.groove.db.model.Track
import com.example.groove.exception.FileStorageException
import com.example.groove.exception.MyFileNotFoundException
import com.example.groove.properties.FileStorageProperties
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import javax.imageio.ImageIO

@Service
class SongIngestionService(
		fileStorageProperties: FileStorageProperties,
		private val ffmpegService: FFmpegService,
		private val fileMetadataService: FileMetadataService,
		private val trackRepository: TrackRepository,
		private val fileStorageService: FileStorageService
) {

	private val fileStorageLocation: Path = Paths.get(fileStorageProperties.tmpDir)
			.toAbsolutePath().normalize()

	init {
		try {
			Files.createDirectories(this.fileStorageLocation)
		} catch (ex: Exception) {
			throw FileStorageException("Could not create the directory where the uploaded files will be stored.", ex)
		}
	}

	private fun storeMultipartFile(file: MultipartFile): String {
		// Discard the file's original name; but keep the extension
		val extension = file.originalFilename!!.split(".").last()
		val fileName = UUID.randomUUID().toString() + "." + extension

		// Copy the song to a temporary location for further processing
		val targetLocation = fileStorageLocation.resolve(fileName)
		Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

		return fileName
	}

	fun storeSongForUser(songFile: MultipartFile, user: User): Track {
		logger.info("Storing song ${songFile.originalFilename} for user: ${user.name}")

		val fileName = storeMultipartFile(songFile)
		val tmpImageFile = ripAndSaveAlbumArt(fileName)

		val track = convertAndSaveTrackForUser(fileName, user)

		if (tmpImageFile != null) {
			fileStorageService.storeAlbumArt(tmpImageFile, track.id)
			// We have stored the songFile in its permanent home. We can delete this tmp songFile
			tmpImageFile.delete()
		}

		return track
	}

	fun storeAlbumArtForTrack(albumArt: MultipartFile, track: Track) {
		logger.info("Storing album artwork ${albumArt.originalFilename} for track ID: ${track.id}")

		val fileName = storeMultipartFile(albumArt)

		val imageFile = fileStorageLocation.resolve(fileName).toFile()
		if (!imageFile.exists()) {
			throw IllegalStateException("Could not store album art for track ID: ${track.id}")
		}

		fileStorageService.storeAlbumArt(imageFile, track.id)
		imageFile.delete()
	}

	// It's important to rip the album art out PRIOR to running the song
	// through FFmpeg to be converted to an .ogg. If you don't, you will
	// get the error "Cannot find comment block (no vorbiscomment header)"
	private fun ripAndSaveAlbumArt(fileName: String): File? {
		val image = fileMetadataService.removeAlbumArtFromFile(fileName)
		return if (image != null) {
			val tmpImageName = UUID.randomUUID().toString() + ".png"
			val outputFile = File(fileStorageLocation.toString() + tmpImageName)
			ImageIO.write(image, "png", outputFile)

			outputFile
		} else {
			null
		}
	}

	@Transactional
	fun convertAndSaveTrackForUser(fileName: String, user: User): Track {
		logger.info("Saving file $fileName for user ${user.name}")

		val convertedFile = ffmpegService.convertTrack(fileName)

		// add the track to database
		val track = fileMetadataService.createTrackFromSongFile(convertedFile, user)
		trackRepository.save(track)

		val renamedSongFile = renameSongFile(convertedFile, track)

		fileStorageService.storeSong(renamedSongFile, track.id)

		// We have stored the file in S3, (or copied it to its final home)
		// We no longer need these files and can clean it up to save space
		convertedFile.delete()
		renamedSongFile.delete()

		return track
	}

	// Now that the file has been saved and has an ID, rename the file on disk to use the ID in the name
	// This way there will never be collision problems with names
	// TODO group the songs in folders of 1000 as with album art
	private fun renameSongFile(sourceFile: File, track: Track): File {
		val destinationFile = File(sourceFile.parent + "/${track.id}.ogg")
		sourceFile.renameTo(destinationFile)

		track.fileName = "${track.id}.ogg"
		trackRepository.save(track)

		return destinationFile
	}

	fun loadFileAsResource(fileName: String): Resource {
		try {
			val filePath = this.fileStorageLocation.resolve(fileName).normalize()
			val resource = UrlResource(filePath.toUri())
			return if (resource.exists()) {
				resource
			} else {
				throw MyFileNotFoundException("File not found $fileName")
			}
		} catch (ex: MalformedURLException) {
			throw MyFileNotFoundException("File not found $fileName", ex)
		}
	}

	companion object {
        private val logger = LoggerFactory.getLogger(SongIngestionService::class.java)
    }
}
