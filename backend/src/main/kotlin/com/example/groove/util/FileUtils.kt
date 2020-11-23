package com.example.groove.util

import com.example.groove.controllers.FileController
import com.example.groove.properties.FileStorageProperties
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.servlet.http.HttpServletResponse

@Service
class FileUtils(private val fileStorageProperties: FileStorageProperties) {
	fun createTemporaryFile(extension: String? = null): File {
		val tmpFileName = UUID.randomUUID().toString() + extension
		return File(fileStorageProperties.tmpDir + tmpFileName)
	}

	fun writeFileToServlet(file: File, servlet: HttpServletResponse, contentType: String) {
		servlet.contentType = contentType
		servlet.setHeader("Content-disposition", """attachment; filename="${file.name}"""")

		val outStream = servlet.outputStream
		val inStream = FileInputStream(file)

		logger.info("Writing file ${file.name} to output stream...")
		outStream.write(inStream.readAllBytes())

		outStream.close()
		inStream.close()
	}

	companion object {
		val logger = logger()
	}
}
