package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MusicProperties {
	@Value("\${spring.data.music.location}")
	val musicDirectoryLocation: String? = null

	@Value("\${spring.data.album.art.location}")
	val albumArtDirectoryLocation: String? = null
	// There will likely be another property here for an S3 storage location
}
