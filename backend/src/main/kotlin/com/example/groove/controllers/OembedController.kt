package com.example.groove.controllers

import com.example.groove.services.EmbedService
import com.example.groove.services.OembedResponse
import com.example.groove.util.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// This controller is currently unused because I see no advantage to using oembed right now.
// Facebook's OGP is better supported and does the same stuff everywhere that I have tried it.
// I wanted to use this because it seemed to let you have a custom video player. But the only place
// that even seems to support oEmbed that I tried (slack) removes <audio> tags and only lets you play
// audio from sources that they have deemed worthy in their almighty wisdom....
// However, it could easily happen that some big messaging provider starts supporting oEmbed and this
// could make sense to turn on and start supporting. So I am leaving the code in place as it is.
@RestController
@RequestMapping("api/oembed")
class OembedController(private val embedService: EmbedService) {

	@GetMapping
	fun getOembedForTrack(@RequestParam url: String): ResponseEntity<OembedResponse?> {
		logger.info("Oembed link requested for URL '$url'")

		val response = embedService.getOembedResponse(url)
				?: return ResponseEntity(HttpStatus.NOT_FOUND)

		return ResponseEntity.ok(response)
	}

	companion object {
		private val logger = logger()
	}
}

