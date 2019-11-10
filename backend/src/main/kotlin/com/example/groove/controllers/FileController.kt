package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.properties.S3Properties
import com.example.groove.services.FileStorageService
import com.example.groove.services.SongIngestionService
import com.example.groove.util.loadLoggedInUser
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import javax.servlet.http.HttpServletResponse
import kotlin.system.measureTimeMillis

@RestController
@RequestMapping("api/file")
class FileController(
		private val songIngestionService: SongIngestionService,
		private val fileStorageService: FileStorageService,
		private val s3Properties: S3Properties
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

	@RequestMapping("/download/{trackId}")
	fun getFile(@PathVariable trackId: Long, response: HttpServletResponse) {
		val file = songIngestionService.createTrackFileWithMetadata(trackId)
		response.contentType = "audio/ogg"
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

	@GetMapping("/link/{trackId}")
	fun getLinksForTrack(@PathVariable trackId: Long): TrackLinks {
		return TrackLinks(
				fileStorageService.getSongLink(trackId, false),
				fileStorageService.getAlbumArtLink(trackId, false),
				s3Properties.awsStoreInS3
		)
	}

	@GetMapping("/track-link/{trackId}")
	fun getLinksForTrackAnonymous(@PathVariable trackId: Long): TrackLinks {
		return TrackLinks(
				fileStorageService.getSongLink(trackId, true),
				null,
				s3Properties.awsStoreInS3
		)
	}

    companion object {
        private val logger = LoggerFactory.getLogger(FileController::class.java)
    }

	data class TrackLinks(
			val songLink: String,
			val albumArtLink: String?,
			val usingS3: Boolean
	)
}
