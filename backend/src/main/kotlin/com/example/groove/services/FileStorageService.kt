package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.exception.FileStorageException
import com.example.groove.exception.MyFileNotFoundException
import com.example.groove.properties.FileStorageProperties
import org.springframework.beans.factory.annotation.Autowired
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
import javax.imageio.ImageIO

@Service
class FileStorageService @Autowired constructor(
		fileStorageProperties: FileStorageProperties,
		private val ffmpegService: FFmpegService,
		private val fileMetadataService: FileMetadataService,
		private val trackRepository: TrackRepository
) {

	private val fileStorageLocation: Path = Paths.get(fileStorageProperties.uploadDir)
			.toAbsolutePath().normalize()

	init {

		try {
			Files.createDirectories(this.fileStorageLocation)
		} catch (ex: Exception) {
			throw FileStorageException("Could not create the directory where the uploaded files will be stored.", ex)
		}
	}

	fun storeSong(file: MultipartFile): Track {
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

			ripAndSaveAlbumArt(fileName)

			// TODO remove old files from the tmp (uploadDir) directory once saving and conversion are finished
			return convertAndSaveTrack(fileName)
		} catch (ex: IOException) {
			throw FileStorageException("Could not store file $fileName. Please try again!", ex)
		}
	}

	// It's important to rip the album art out PRIOR to running the song
	// through FFmpeg to be converted to an .ogg. If you don't, you will
	// get the error "Cannot find comment block (no vorbiscomment header)"
	private fun ripAndSaveAlbumArt(fileName: String) {
		val image = fileMetadataService.removeAlbumArtFromFile(fileName)
		if (image != null) {
			// TODO put this somewhere more intelligent so that we can actually use it
			val outputFile = File("C:\\Users\\mobius\\tmp\\test.png")
			ImageIO.write(image, "png", outputFile)
		}
	}

	@Transactional
	fun convertAndSaveTrack(fileName: String): Track {
		// convert to .ogg
		// TODO this also moves the file from the uploadDir to its final home in the music dir
		// TODO we probably don't want the FFmpeg service responsible for moving the file, just converting it
		val convertedFileName = ffmpegService.convertTrack(fileName)

		// add the track to database
		val track = fileMetadataService.createTrackFromFileName(convertedFileName)
		trackRepository.save(track)

		return track
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
}
