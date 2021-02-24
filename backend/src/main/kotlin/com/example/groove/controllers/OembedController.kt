package com.example.groove.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/oembed")
class OembedController {

	@GetMapping("/track/{trackId}")
	fun getOembedForTrack(@PathVariable trackId: Long): OembedResponse {
		return OembedResponse()
	}
}

data class OembedResponse(
		val version: String = "1.0",
		val type: String = "video",
		val width: Int = 240,
		val height: Int = 180,
		val title: String = "This is a test",

		@JsonProperty("provider_name")
		val providerName: String = "Gorilla Groove",

		@JsonProperty("provider_url")
		val providerUrl: String = "https://gorillagroove.net",

		val html: String = """
			<div>
			Damn this is a sick video
			</div>
		""".trimIndent()
)
