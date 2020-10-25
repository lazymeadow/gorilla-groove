package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.S3Properties
import com.example.groove.services.ArtSize
import com.example.groove.services.FileStorageService
import com.example.groove.services.SongIngestionService
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import javax.servlet.http.HttpServletResponse
import kotlin.system.measureTimeMillis
import org.springframework.http.ResponseEntity
import java.nio.file.Files
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.io.File
import java.nio.file.Paths


@RestController
@RequestMapping("api/file")
class FileController(
		private val songIngestionService: SongIngestionService,
		private val fileStorageService: FileStorageService,
		private val s3Properties: S3Properties,
		private val fileStorageProperties: FileStorageProperties
) {

	// Example cURL command for uploading a file
	// curl -H "Content-Type: multipart/form-data" -H "Authorization: Bearer df86c467-d940-4239-889f-4d72329f0ba4"
	// -F "file=@C:/Users/user/Music/Song.mp3"  http://localhost:8080/api/file/upload
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): Track {
		// FIXME file.name does not appear to be anything useful
		logger.info("Beginning file upload: ${file.name}")

		var track: Track? = null
		val timeToUpload = measureTimeMillis {
			track = songIngestionService.storeSongForUser(file, loadLoggedInUser())
		}

		logger.info("File upload complete for ${file.name} in $timeToUpload")
		return track!!
    }

	@GetMapping("/download/{trackId}")
	fun getFile(
			@PathVariable trackId: Long,
			@RequestParam audioFormat: AudioFormat = AudioFormat.OGG,
			response: HttpServletResponse
	) {
		val file = songIngestionService.createTrackFileWithMetadata(trackId, audioFormat)
		response.contentType = audioFormat.contentType
		response.setHeader("Content-disposition", """attachment; filename="${file.name}"""")

		val outStream = response.outputStream
		val inStream = FileInputStream(file)

		logger.info("Writing song data for ${file.name} to output stream...")
		outStream.write(inStream.readAllBytes())

		outStream.close()
		inStream.close()

		logger.info("Deleting temporary file")
		file.delete()
	}

	@GetMapping("/download-apk")
	fun downloadApk(): ResponseEntity<Resource> {
		val path = fileStorageProperties.apkDownloadDir
				?: throw IllegalStateException("No APK location has been configured!")

		val apkFile = File(path)

		if (!apkFile.exists()) {
			throw IllegalStateException("No APK exists at the specified location '$path'!")
		}

		val resource = ByteArrayResource(
				Files.readAllBytes(Paths.get(apkFile.absolutePath))
		)

		val headers = HttpHeaders()
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=groove.apk")

		return ResponseEntity.ok()
				.headers(headers)
				.contentLength(apkFile.length())
				.contentType(MediaType.parseMediaType("application/octet-stream"))
				.body(resource)
	}

	@GetMapping("/link/{trackId}")
	fun getLinksForTrack(
			@PathVariable trackId: Long,
			@RequestParam(defaultValue = "OGG") audioFormat: AudioFormat,
			@RequestParam(defaultValue = "LARGE") artSize: ArtSize
	): TrackLinks {
		val user = loadLoggedInUser()
		logger.info("Links requested for Track: $trackId from user: ${user.name} with format: $audioFormat and art size: $artSize")

		val artLink = fileStorageService.getAlbumArtLink(trackId, false, artSize)

		val returnedArtLink = if (artLink.isEmpty()) {
			// FIXME Need to update iOS to be able to handle empty / null album art
			if (user.currentAuthToken!!.device!!.deviceType == DeviceType.IPHONE) {
				"https://gorillagroove.net/api/somethingfake"
			} else {
				// We get back an empty string if it's invalid for laziness reasons.
				// But we want to return null to consumers as it's the right thing to do.
				null
			}
		} else {
			artLink
		}

		return TrackLinks(
				fileStorageService.getSongLink(trackId, false, audioFormat),
				returnedArtLink,
				s3Properties.awsStoreInS3
		)
	}

	@GetMapping("/track-link/{trackId}")
	fun getLinksForTrackAnonymous(@PathVariable trackId: Long): TrackLinks {
		logger.info("Anonymous track links were requested for Track ID: $trackId")

		return TrackLinks(
				fileStorageService.getSongLink(trackId, true, AudioFormat.OGG),
				null,
				s3Properties.awsStoreInS3
		)
	}

    companion object {
        private val logger = logger()
    }

	data class TrackLinks(
			val songLink: String,
			val albumArtLink: String?,
			val usingS3: Boolean
	)
}
