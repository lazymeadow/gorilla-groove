package com.example.groove.services

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.util.createMapper
import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate
import java.sql.Timestamp

class AppleMetadataService(private val restTemplate: RestTemplate) {
	private val objectMapper = createMapper()

	// TODO we can only search iTunes ~20 times a minute, so throttling ourselves in some way here would be good
	fun getMetadataByTrackNameAndArtist(name: String, artist: String): MetadataResponseDTO? {
		// Because we can't search iTunes for both terms at once, we have to grab everything from one term,
		// and then parse the other one ourselves. There are a lot of potential ways this could go wrong, so
		// for increased resiliency I have opted to try to search twice- first by artist and then by title
		val appleTrack = (matchByArtist(name, artist) ?: matchByName(name, artist)) ?: run {
			logger.warn("Failed to find iTunes metadata for $artist - $name!")
			return null
		}

		return MetadataResponseDTO.fromAppleTrack(appleTrack)
	}

	private fun createBaseBuilder(): URIBuilder {
		return URIBuilder()
				.setScheme("https")
				.setHost("itunes.apple.com")
				.setPath("search")
				.addParameter("entity", "musicTrack")
	}

	private fun matchByArtist(name: String, artist: String): AppleTrack? {
		val url = createBaseBuilder()
				.addParameter("attribute", "artistTerm")
				.addParameter("term", artist)
				.toString()

		// Apple gives us a weird attachment response and the automatic deserialization barfs on it if we
		// try to go directly to an AppleResponse. So just take it as a string and call Jackson ourselves
		val rawResponse = restTemplate.getForObject(url, String::class.java)!!
		val response = objectMapper.readValue(rawResponse, AppleResponse::class.java)

		println(response)

		return response.results.find { it.trackName == name }
	}

	private fun matchByName(name: String, artist: String): AppleTrack? {
		val url = createBaseBuilder()
				.addParameter("attribute", "songTerm")
				.addParameter("term", name)
				.toString()

		val rawResponse = restTemplate.getForObject(url, String::class.java)!!
		val response = objectMapper.readValue(rawResponse, AppleResponse::class.java)

		return response.results.find { it.artistName == artist }
	}

	data class AppleResponse(
			val resultCount: Int,
			val results: List<AppleTrack>
	)

	data class AppleTrack(
			val wrapperType: String, // "track"
			val kind: String, // "song" (might also be possible to be "music video" or something
			val artistName: String,
			val collectionName: String, // Album name
			val trackName: String,
			val artworkUrl100: String, // This is the largest size in the response. But there are larger images offered
			val releaseDate: Timestamp, // 2003-12-17T12:00:00Z
			val trackNumber: Int,
			val primaryGenreName: String
	)

	private fun MetadataResponseDTO.Companion.fromAppleTrack(appleTrack: AppleTrack): MetadataResponseDTO {
		return MetadataResponseDTO(
				name = appleTrack.trackName,
				artist = appleTrack.artistName,
				album = appleTrack.collectionName,
				genre = appleTrack.primaryGenreName,
				releaseYear = appleTrack.releaseDate.toLocalDateTime().year,
				trackNumber = appleTrack.trackNumber,
				albumArtLink = appleTrack.artworkUrl100
		)
	}

	companion object {
		val logger = LoggerFactory.getLogger(AppleMetadataService::class.java)!!
	}
}
