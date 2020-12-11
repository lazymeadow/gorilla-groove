package com.example.groove

import com.example.groove.properties.SpotifyApiProperties
import com.example.groove.properties.YouTubeApiProperties
import com.example.groove.services.SpotifyApiClient
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate

class Woohoo {

	class TestYouTubeApiProperties: YouTubeApiProperties() {
		override val youtubeApiKey: String?
			get() = "EXAMPLE_API_KEY"
	}

	@Test
	fun waaWaaWeeWaa() {
		val properties = SpotifyApiProperties()
		val api = SpotifyApiClient(properties, RestTemplate())

		val response = api.getMetadataByTrackArtistAndName("Ayumi Hamasaki", "Alterna", limit = 5)
		println(response)

//		val response = api.getSpotifyArtistId("LIONE")
//		val response = api.getAlbumsByArtistId("4Zv449cAssgslcvInvc0wa")
//		val response = api.getTracksFromAlbumIds(setOf("62MQvUh5IaT3r0bXgGqImE","6EZ7bYEiEunzlIpcM25GuG","046B22KTV1C8Hsg5XXkxjN"))

//		val testDate = LocalDate.parse("2020-02-10")
//		val response = api.getSongsByArtist("sad alex", testDate)

//		println(response.joinToString("\n"))
	}
}
