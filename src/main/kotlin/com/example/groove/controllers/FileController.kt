package com.example.groove.controllers

import com.example.groove.db.dao.TrackRepository
import com.example.groove.payload.UploadFileResponse
import com.example.groove.services.FFmpegService
import com.example.groove.services.FileMetadataService
import com.example.groove.services.FileStorageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.servlet.http.HttpServletRequest
import java.io.IOException
import java.util.Arrays
import java.util.stream.Collectors

@RestController
class FileController {

    @Autowired
    private val fileStorageService: FileStorageService? = null
    @Autowired private val fFmpegService: FFmpegService? = null
    @Autowired private val fileMetadataService : FileMetadataService? = null
    @Autowired private val trackRepository : TrackRepository? = null

    @PostMapping("/uploadFile")
    fun uploadFile(@RequestParam("file") file: MultipartFile): UploadFileResponse {
        val fileName = fileStorageService!!.storeFile(file)



        // Might need to alter this to allow for different file types upon download
        val fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString()


        // convert to .ogg
        fFmpegService!!.convertTrack(fileName)

        //Strip current file extension and append .ogg
        val convertedFileName = fileName.substringBeforeLast('.') + ".ogg"

        // add to track to database
        val track = fileMetadataService!!.createTrackFromFileName(convertedFileName)
        trackRepository!!.save(track)



        return UploadFileResponse(fileName, fileDownloadUri,
                file.contentType!!, file.size)
    }

    @PostMapping("/uploadMultipleFiles")
    fun uploadMultipleFiles(@RequestParam("files") files: Array<MultipartFile>): List<UploadFileResponse> {
        return (Arrays.asList(*files)
                .stream()
                .map<Any> { file -> uploadFile(file) }
                .collect(Collectors.toList()) as List<UploadFileResponse>?)!!
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    fun downloadFile(@PathVariable fileName: String, request: HttpServletRequest): ResponseEntity<Resource> {
        // Load file as Resource
        val resource = fileStorageService!!.loadFileAsResource(fileName)

        // Try to determine file's content type
        var contentType: String? = null
        try {
            contentType = request.servletContext.getMimeType(resource.getFile().getAbsolutePath())
        } catch (ex: IOException) {
            logger.info("Could not determine file type.")
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream"
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FileController::class.java)
    }
}