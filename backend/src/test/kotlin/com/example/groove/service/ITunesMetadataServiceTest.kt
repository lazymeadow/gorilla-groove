package com.example.groove.service

import com.example.groove.services.ITunesMetadataService
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder

class ITunesMetadataServiceTest {

	@Test // Not actually a unit test, just an easy way to test the iTunes API
	fun `Run the search`() {
		val service = ITunesMetadataService(RestTemplateBuilder().build())
		val result = service.getMetadataByTrackArtistAndName("bloodhound gang", "clean up in aisle sexy", null)
		println(result)
	}
}