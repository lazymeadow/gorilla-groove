package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class YouTubeDlProperties {
	@Value("\${spring.data.youtubedl.binary.location}")
	val youtubeDlBinaryLocation: String? = null
}
