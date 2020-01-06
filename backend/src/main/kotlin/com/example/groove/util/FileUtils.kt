package com.example.groove.util

import com.example.groove.properties.FileStorageProperties
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class FileUtils(private val fileStorageProperties: FileStorageProperties) {
	fun createTemporaryFile(extension: String? = null): File {
		val tmpFileName = UUID.randomUUID().toString() + extension
		return File(fileStorageProperties.tmpDir + tmpFileName)
	}
}
