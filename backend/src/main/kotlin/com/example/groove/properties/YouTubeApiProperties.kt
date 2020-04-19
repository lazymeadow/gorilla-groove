package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class YouTubeApiProperties {
	@Value("\${spring.data.youtube.api.key:#{null}}")
	val youtubeApiKey: String? = null
		get() = when (field) {
			null -> throw IllegalStateException("Missing Youtube API Key in configuration! Configure with 'spring.data.youtube.api.key'")
			"EXAMPLE_KEY" -> throw IllegalStateException("Youtube API Key is using the default 'EXAMPLE_KEY'! Configure 'spring.data.youtube.api.key'")
			else -> field
		}
}
