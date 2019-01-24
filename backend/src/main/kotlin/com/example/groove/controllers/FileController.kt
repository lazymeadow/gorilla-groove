package com.example.groove.controllers

import com.example.groove.services.FileStorageService
import com.example.groove.util.loadLoggedInUser
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest
import java.io.IOException
import kotlin.system.measureTimeMillis

@RestController
@RequestMapping("api/file")
class FileController(
		private val fileStorageService: FileStorageService
) {

	// Example cURL command for uploading a file
	// curl -H "Content-Type: multipart/form-data" -H "Authorization: Bearer df86c467-d940-4239-889f-4d72329f0ba4"
	// -F "file=@C:/Users/user/Music/Song.mp3"  http://localhost:8080/api/file/upload
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
		logger.info("Beginning file upload: ${file.name}")

		// TODO I imagine we will return this to the front end so it can add it
		val timeToUpload = measureTimeMillis {
			val track = fileStorageService.storeSongForUser(file, loadLoggedInUser())
		}

		logger.info("File upload complete for ${file.name} in $timeToUpload")

        return ResponseEntity(HttpStatus.CREATED)
    }

    @PostMapping("/upload-multiple-files")
    fun uploadMultipleFiles(@RequestParam("files") files: Array<MultipartFile>): ResponseEntity<String> {
        files.map { uploadFile(it) }

        return ResponseEntity(HttpStatus.CREATED)
    }

    @GetMapping("/download")
    fun downloadFile(@PathVariable fileName: String, request: HttpServletRequest): ResponseEntity<Resource> {
        // Load file as Resource
        val resource = fileStorageService.loadFileAsResource(fileName)

        // Try to determine file's content type
        val contentType = try {
            request.servletContext.getMimeType(resource.file.absolutePath)
        } catch (ex: IOException) {
            logger.info("Could not determine file type.")
            "application/octet-stream"
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${resource.filename}\"")
                .body(resource)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileController::class.java)
    }
}
