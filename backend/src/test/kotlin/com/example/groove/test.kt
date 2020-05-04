package com.example.groove

import com.example.groove.properties.YouTubeApiProperties
import com.example.groove.services.YoutubeApiClient
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate

class Woohoo {

	class TestYouTubeApiProperties: YouTubeApiProperties() {
		override val youtubeApiKey: String?
			get() = "EXAMPLE_API_KEY"
	}

	@Test
	fun hey() {
		val api = YoutubeApiClient(TestYouTubeApiProperties(), RestTemplate())
		val response = api.findVideos(searchTerm = "deadmau5")

		println(response.videos.size)
	}

	@Test
	fun listen() {
		val api = YoutubeApiClient(TestYouTubeApiProperties(), RestTemplate())
		val response = api.findVideos(channelId = "UCpDJl2EmP7Oh90Vylx0dZtA")

		println(response.videos)
	}
}
