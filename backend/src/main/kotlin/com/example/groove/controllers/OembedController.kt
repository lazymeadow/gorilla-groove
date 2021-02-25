package com.example.groove.controllers

import com.example.groove.services.TrackService
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.logger
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/oembed")
class OembedController(private val trackService: TrackService) {

	@GetMapping
	fun getOembedForTrack(@RequestParam url: String): ResponseEntity<OembedResponse?> {
		logger.info("Returning oembed link for url: '$url'")
		val trackId = url.split('/').lastOrNull()?.toLong() ?: run {
			logger.error("Could not parse a track ID from oembed URL! URL was: '$url'")
			return ResponseEntity(HttpStatus.BAD_REQUEST)
		}

		// It's probably safer to just always use MP3 here. As much as I want to use an OGG, not all clients may support it
		// and we have no way of knowing what our client necessarily is. Could be a damn Safari web browser
		val trackInfo = try {
			trackService.getPublicTrackInfo(trackId, true, AudioFormat.MP3)
		} catch (e: Throwable) {
			logger.warn("No track info found for oembed link for track ID: $trackId. It may have expired.")
			return ResponseEntity(HttpStatus.NOT_FOUND)
		}

		return ResponseEntity.ok(OembedResponse())
	}

	companion object {
		private val logger = logger()
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
