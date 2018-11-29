package com.example.groove.controllers

import com.example.groove.db.dao.UserLibraryRepository
import com.example.groove.db.model.UserLibrary
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

@RestController
@RequestMapping("file")
class FileController(
		private val fileStorageService: FileStorageService,
		private val userLibraryRepository: UserLibraryRepository
) {

	// Example cURL command for uploading a file
	// curl -H "Content-Type: multipart/form-data" -H "Authorization: Bearer df86c467-d940-4239-889f-4d72329f0ba4"
	// -F "file=@C:/Users/user/Music/Song.mp3"  http://localhost:8080/api/file/upload
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val track = fileStorageService.storeSong(file)
		userLibraryRepository.save(UserLibrary(user = loadLoggedInUser(), track = track))

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
