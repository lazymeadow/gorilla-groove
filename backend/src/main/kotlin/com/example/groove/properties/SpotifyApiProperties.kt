package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SpotifyApiProperties {
	@Value("\${spring.data.spotify.api.secret:#{null}}")
	val spotifyApiSecret: String? = null
		get() = when (field) {
			null -> throw IllegalStateException("Missing Spotify API Key in configuration! Configure with 'spring.data.spotify.api.secret'")
			"EXAMPLE_KEY" -> throw IllegalStateException("Youtube API Key is using the default 'EXAMPLE_KEY'! Configure 'spring.data.spotify.api.secret'")
			else -> field
		}
}
