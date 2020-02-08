package com.example.groove.properties


import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class FileStorageProperties {
	@Value("\${file.storage.location:#{null}}")
	private val rootStorageLocation: String? = null

	@Value("\${file.tmpDir:#{null}}")
    val tmpDir: String? = null
		get() = field ?: "${rootStorageLocation!!}/tmp/"

	@Value("\${file.apk.download.path:#{null}}")
	val apkDownloadDir: String? = null
		get() = field ?: "${rootStorageLocation!!}/"

	@Value("\${spring.data.music.location:#{null}}")
	val musicDirectoryLocation: String? = null
		get() = field ?: "${rootStorageLocation!!}/music/"

	@Value("\${spring.data.album.art.location:#{null}}")
	val albumArtDirectoryLocation: String? = null
		get() = field ?: "${rootStorageLocation!!}/art/"
}
