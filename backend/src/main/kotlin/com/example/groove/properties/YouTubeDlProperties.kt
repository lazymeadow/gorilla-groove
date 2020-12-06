package com.example.groove.properties

import com.example.groove.util.endWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class YouTubeDlProperties {
	@Value("\${spring.data.youtubedl.binary.location:#{null}}")
	val youtubeDlBinaryLocation: String? = null
		get() = field?.endWith("/")
}
