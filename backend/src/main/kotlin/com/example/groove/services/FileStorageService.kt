package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.User
import com.example.groove.db.model.Track
import com.example.groove.exception.FileStorageException
import com.example.groove.exception.MyFileNotFoundException
import com.example.groove.properties.FFmpegProperties
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.MusicProperties
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

@Service
class FileStorageService(
		fileStorageProperties: FileStorageProperties,
		musicProperties: MusicProperties,
		private val ffmpegService: FFmpegService,
		private val fileMetadataService: FileMetadataService,
		private val trackRepository: TrackRepository,
		private val ffmpegProperties: FFmpegProperties
) {

	private val fileStorageLocation: Path = Paths.get(fileStorageProperties.uploadDir)
			.toAbsolutePath().normalize()

	private val albumArtLocation: Path = Paths.get(musicProperties.albumArtDirectoryLocation)
			.toAbsolutePath().normalize()

	init {

		try {
			Files.createDirectories(this.fileStorageLocation)
		} catch (ex: Exception) {
			throw FileStorageException("Could not create the directory where the uploaded files will be stored.", ex)
		}
	}

	fun storeSongForUser(file: MultipartFile, user: User): Track {
		// Normalize file name
		val fileName = StringUtils.cleanPath(file.originalFilename!!)

		try {
			// Check if the file's name contains invalid characters
			if (fileName.contains("..")) {
				throw FileStorageException("Sorry! Filename contains invalid path sequence $fileName")
			}

			// TODO: Change this to allow unique files with same filename
			// Copy the song to a temporary location for further processing
			val targetLocation = fileStorageLocation.resolve(fileName)
			Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

			val tmpImageFile = ripAndSaveAlbumArt(fileName)

			// TODO remove old files from the tmp (uploadDir) directory once saving and conversion are finished
			val track = convertAndSaveTrackForUser(fileName, user)

			if (tmpImageFile != null) {
				moveAlbumArt(tmpImageFile, track.id)
			}

			return track
		} catch (ex: IOException) {
			throw FileStorageException("Could not store file $fileName. Please try again!", ex)
		}
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

	fun moveAlbumArt(tmpAlbumArt: File, trackId: Long) {
		val parentDirectoryName = trackId / 1000 // Only put 1000 album art in a single directory for speed
		val destinationFile = File("$albumArtLocation/$parentDirectoryName/$trackId.png")

		// The parent directory might not be made. Make it if it doesn't exist
		destinationFile.parentFile.mkdirs()

		tmpAlbumArt.renameTo(destinationFile)
	}

	fun copyAlbumArt(trackSourceId: Long, trackDestinationId: Long) {
		val sourceDirectory = trackSourceId / 1000
		val sourceFile = File("$albumArtLocation/$sourceDirectory/$trackSourceId.png")

		if (!sourceFile.exists()) {
			return
		}

		val destinationDirectory = trackDestinationId / 1000
		val destinationFile = File("$albumArtLocation/$destinationDirectory/$trackDestinationId.png")

		sourceFile.copyTo(destinationFile)
	}

	@Transactional
	fun convertAndSaveTrackForUser(fileName: String, user: User): Track {
		var convertedFileName: String? = null

		// Timing for benchmarks while messing with AWS. Remove later
		val timeToConvert = measureTimeMillis {
			// convert to .ogg
			// TODO this also moves the file from the uploadDir to its final home in the music dir
			// TODO we probably don't want the FFmpeg service responsible for moving the file, just converting it

			convertedFileName = ffmpegService.convertTrack(fileName)
		}

		logger.info("FFmpeg convert of $fileName done in $timeToConvert")

		// add the track to database
		val track = fileMetadataService.createTrackFromFileName(convertedFileName!!, user)
		trackRepository.save(track)

		// Now that the file has been saved and has an ID, rename the file on disk to use the ID in the name
		// This way there will never be collision problems with names
		renameSongFile(track)

		return track
	}

	// TODO group the songs in folders of 1000 as with album art
	private fun renameSongFile(track: Track) {
		val outputDir = ffmpegProperties.ffmpegOutputLocation
		val sourceFile = File(outputDir + track.fileName)

		val destinationFile = File("$outputDir" + "${track.id}.ogg")
		sourceFile.renameTo(destinationFile)

		track.fileName = "${track.id}.ogg"
		trackRepository.save(track)
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
        private val logger = LoggerFactory.getLogger(FileStorageService::class.java)
    }
}
