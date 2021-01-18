package com.example.groove.properties


import com.example.groove.util.endWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class FileStorageProperties {
	@Value("\${file.storage.location:#{null}}")
	private val rootStorageLocation: String? = null
		get() = field?.endWith("/")

	@Value("\${file.tmpDir:#{null}}")
    val tmpDir: String? = null
		get() = (field ?: "${rootStorageLocation!!}tmp").endWith("/")

	@Value("\${spring.data.music.location:#{null}}")
	val musicDirectoryLocation: String? = null
		get() = (field ?: "${rootStorageLocation!!}music").endWith("/")

	@Value("\${spring.data.album.art.location:#{null}}")
	val albumArtDirectoryLocation: String? = null
		get() = (field ?: "${rootStorageLocation!!}art").endWith("/")

	@Value("\${spring.data.crash.report.location:#{null}}")
	val crashReportLocation: String? = null
		get() = (field ?: "${rootStorageLocation!!}crashes").endWith("/")
}
