package com.example.groove.properties


import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class FileStorageProperties {
	@Value("\${file.tmpDir}")
    val tmpDir: String? = null

	@Value("\${file.apk.download.path}")
	val apkDownloadDir: String? = null
}
