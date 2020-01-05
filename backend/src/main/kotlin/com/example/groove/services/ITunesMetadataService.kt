package com.example.groove.services

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.util.createMapper
import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.sql.Timestamp

@Service
class ITunesMetadataService(private val restTemplate: RestTemplate) {
	private val objectMapper = createMapper()

	// TODO we can only search iTunes ~20 times a minute, so throttling ourselves in some way here would be good
	fun getMetadataByTrackArtistAndName(artist: String, name: String): MetadataResponseDTO? {
		val searchName = name.toLowerCase()
		val searchArtist = artist.toLowerCase()
		// Because we can't search iTunes for both terms at once, we have to grab everything from one term,
		// and then parse the other one ourselves. There are a lot of potential ways this could go wrong, so
		// for increased resiliency I have opted to try to search twice- first by artist and then by title
		val iTunesTrack = (matchByArtist(searchArtist, searchName) ?: matchByName(searchArtist, searchName)) ?: run {
			logger.warn("Failed to find iTunes metadata for $artist - $name!")
			return null
		}

		return MetadataResponseDTO.fromITunesTrack(iTunesTrack)
	}

	private fun createBaseBuilder(): URIBuilder {
		return URIBuilder()
				.setScheme("https")
				.setHost("itunes.apple.com")
				.setPath("search")
				.addParameter("entity", "musicTrack")
				 // 200 is the max they allow. For artists with a lot of songs this gives us the best chance of finding a match
				.addParameter("limit", "200")
	}

	private fun matchByArtist(artist: String, name: String): ITunesTrack? {
		val url = createBaseBuilder()
				.addParameter("attribute", "artistTerm")
				.addParameter("term", artist)
				.toString()

		// Apple gives us a weird attachment response and the automatic deserialization barfs on it if we
		// try to go directly to an AppleResponse. So just take it as a string and call Jackson ourselves
		logger.info("Searching for metadata by artist: $artist - $name. Sending URL $url")
		val rawResponse = restTemplate.getForObject(url, String::class.java)!!
		val response = try {
			objectMapper.readValue(rawResponse, AppleResponse::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize iTunes response from artist", e)
			logger.error("Raw response: $rawResponse")
			return null
		}

		return response.results.find { it.trackName.toLowerCase() == name }
				?: run {
					logger.warn("Failed to match (by artist): $artist - $name")
					null
				}
	}

	private fun matchByName(artist: String, name: String): ITunesTrack? {
		val url = createBaseBuilder()
				.addParameter("attribute", "songTerm")
				.addParameter("term", name)
				.toString()

		logger.info("Searching for metadata by name: $artist - $name. Sending URL $url")
		val rawResponse = restTemplate.getForObject(url, String::class.java)!!
		val response = try {
			objectMapper.readValue(rawResponse, AppleResponse::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize iTunes response from name", e)
			logger.error("Raw response: $rawResponse")
			return null
		}

		return response.results.find { it.artistName.toLowerCase() == artist }
				?: run {
					logger.warn("Failed to match (by name): $artist - $name")
					null
				}
	}

	data class AppleResponse(
			val resultCount: Int,
			val results: List<ITunesTrack>
	)

	data class ITunesTrack(
			val wrapperType: String, // "track"
			val kind: String, // "song" (might also be possible to be "music video" or something
			val artistName: String,
			val collectionName: String?, // Album name (music videos don't have this, and are deserialized)
			val trackName: String,
			val artworkUrl100: String, // This is the largest size in the response. But there are larger images offered
			val releaseDate: Timestamp, // 2003-12-17T12:00:00Z
			val trackNumber: Int,
			val primaryGenreName: String
	)

	private fun MetadataResponseDTO.Companion.fromITunesTrack(ITunesTrack: ITunesTrack): MetadataResponseDTO {
		return MetadataResponseDTO(
				name = ITunesTrack.trackName,
				artist = ITunesTrack.artistName,
				album = ITunesTrack.collectionName!!, // Non-music videos should have this link
				genre = ITunesTrack.primaryGenreName,
				releaseYear = ITunesTrack.releaseDate.toLocalDateTime().year,
				trackNumber = ITunesTrack.trackNumber,
				albumArtLink = ITunesTrack.artworkUrl100 // TODO Need to try to get larger size
		)
	}

	companion object {
		val logger = LoggerFactory.getLogger(ITunesMetadataService::class.java)!!
	}
}
