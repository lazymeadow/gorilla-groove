package com.example.groove

import com.example.groove.services.YoutubeApiClient
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate

class Woohoo {
	@Test
	fun hey() {
		val api = YoutubeApiClient(RestTemplate())
		val response = api.findVideos("deadmau5")

		println(response.videos.size)
	}
}