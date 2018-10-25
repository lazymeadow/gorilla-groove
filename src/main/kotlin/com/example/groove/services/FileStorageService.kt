package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
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
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Service
class FileStorageService @Autowired constructor(
        fileStorageProperties: FileStorageProperties,
        private val fFmpegService: FFmpegService,
        private val fileMetadataService: FileMetadataService,
        private val trackRepository: TrackRepository
) {

    private val fileStorageLocation: Path

    init {
        this.fileStorageLocation = Paths.get(fileStorageProperties.uploadDir)
                .toAbsolutePath().normalize()

        try {
            Files.createDirectories(this.fileStorageLocation)
        } catch (ex: Exception) {
            throw FileStorageException("Could not create the directory where the uploaded files will be stored.", ex)
        }
    }

    fun storeFile(file: MultipartFile): String {
        // Normalize file name
        val fileName = StringUtils.cleanPath(file.originalFilename!!)

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw FileStorageException("Sorry! Filename contains invalid path sequence $fileName")
            }

            // TODO: Change this to allow unique files with same filename
            // Copy file to the target location (Replacing existing file with the same name)
            val targetLocation = this.fileStorageLocation.resolve(fileName)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

            return convertAndSaveTrack(fileName)
        } catch (ex: IOException) {
            throw FileStorageException("Could not store file $fileName. Please try again!", ex)
        }
    }

    @Transactional
    fun convertAndSaveTrack(fileName: String): String {
        // convert to .ogg
        fFmpegService.convertTrack(fileName)

        //Strip current file extension and append .ogg
        val convertedFileName = fileName.substringBeforeLast('.') + ".ogg"

        // add to track to database
        val track = fileMetadataService.createTrackFromFileName(convertedFileName)
        trackRepository.save(track)

        return convertedFileName
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
