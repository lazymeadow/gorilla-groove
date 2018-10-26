package com.example.groove.controllers

import com.example.groove.services.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
class FileController @Autowired constructor(
        private val fileStorageService: FileStorageService
) {

    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        fileStorageService.storeFile(file)

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
            request.servletContext.getMimeType(resource.getFile().getAbsolutePath())
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
