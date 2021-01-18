package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.properties.FileStorageProperties
import com.example.groove.services.ArtSize
import com.example.groove.services.FileStorageService
import com.example.groove.services.SongIngestionService
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.FileUtils
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
		private val fileStorageProperties: FileStorageProperties,
		private val fileUtils: FileUtils
) {

	// Example cURL command for uploading a file
	// curl -H "Content-Type: multipart/form-data" -H "Authorization: Bearer df86c467-d940-4239-889f-4d72329f0ba4"
	// -F "file=@C:/Users/user/Music/Song.mp3"  http://localhost:8080/api/file/upload
    @PostMapping("/upload")
    fun uploadTrack(@RequestParam("file") file: MultipartFile): Track {
		// FIXME file.name does not appear to be anything useful
		logger.info("Beginning file upload: ${file.name}")

		var track: Track?
		val timeToUpload = measureTimeMillis {
			track = songIngestionService.storeSongForUser(file, loadLoggedInUser())
		}

		logger.info("File upload complete for ${file.name} in $timeToUpload ms")
		return track!!
    }

	@GetMapping("/download/{trackId}")
	fun getFile(
			@PathVariable trackId: Long,
			@RequestParam audioFormat: AudioFormat = AudioFormat.OGG,
			response: HttpServletResponse
	) {
		val file = songIngestionService.createTrackFileWithMetadata(trackId, audioFormat)
		fileUtils.writeFileToServlet(file, response, audioFormat.contentType)

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

	@PostMapping("/link/{trackId}")
	fun forceLinksForTrack(@PathVariable trackId: Long) {
		logger.info("User ${loadLoggedInUser().name} is forcing link generation for track $trackId")

		// We don't know what device someone is going to listen to this on, so we have to generate MP3 and OGG as
		// not all browsers support OGG. We don't generate SMALL art size here as those are currently only used by mobile
		// TODO a good improvement here would be to force new links to be generated instead. This would set the track
		// expiration out to 4 hours, so you can't request track links, link a song to someone who does not have an account,
		// and then have the track expire minutes later like it could today.
		fileStorageService.getSongLink(trackId, false, AudioFormat.MP3)
		fileStorageService.getSongLink(trackId, false, AudioFormat.OGG)
		fileStorageService.getAlbumArtLink(trackId, false, ArtSize.LARGE)
	}

	enum class LinkFetchType { ART, SONG, BOTH }

	@GetMapping("/link/{trackId}")
	fun getLinksForTrack(
			@PathVariable trackId: Long,
			@RequestParam(defaultValue = "BOTH") linkFetchType: LinkFetchType,
			@RequestParam(defaultValue = "OGG") audioFormat: AudioFormat,
			@RequestParam(defaultValue = "LARGE") artSize: ArtSize
	): TrackLinks {
		val user = loadLoggedInUser()
		logger.info("$linkFetchType link(s) requested for Track: $trackId from user: ${user.name} with format: $audioFormat and art size: $artSize. (${user.currentAuthToken?.device?.deviceType})")

		val artLink = if (linkFetchType == LinkFetchType.ART || linkFetchType == LinkFetchType.BOTH) {
			fileStorageService.getAlbumArtLink(trackId, false, artSize)
		} else {
			""
		}

		val returnedArtLink = if (artLink.isEmpty()) {
			// We get back an empty string if it's invalid for laziness reasons.
			// But we want to return null to consumers as it's the right thing to do.
			null
		} else {
			artLink
		}

		val songLink = if (linkFetchType == LinkFetchType.SONG || linkFetchType == LinkFetchType.BOTH) {
			fileStorageService.getSongLink(trackId, false, audioFormat)
		} else {
			null
		}

		return TrackLinks(songLink, returnedArtLink)
	}

	@GetMapping("/track-link/{trackId}")
	fun getLinksForTrackAnonymous(@PathVariable trackId: Long): TrackLinks {
		logger.info("Anonymous track links were requested for Track ID: $trackId")

		return TrackLinks(
				songLink = fileStorageService.getSongLink(trackId, true, AudioFormat.OGG),
				albumArtLink = null
		)
	}

    companion object {
        private val logger = logger()
    }

	data class TrackLinks(
			val songLink: String?,
			val albumArtLink: String?
	)
}
