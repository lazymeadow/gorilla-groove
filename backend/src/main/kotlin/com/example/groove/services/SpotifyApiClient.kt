package com.example.groove.services

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.util.*
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.http.client.utils.URIBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import kotlin.concurrent.schedule


private val objectMapper = createMapper()

// It's an error to request more than this
private const val REQUEST_SIZE_LIMIT = 49

// Even if more results exist, spotify gets mad at you if you look beyond the 2000th result
private const val MAX_RESULTS = 2000

@Service
class SpotifyApiClient(
		private val restTemplate: RestTemplate
) {

	private val spotifyTokenManager: SpotifyTokenManager = SpotifyTokenManager(restTemplate)

	private val authHeader: String
		get() = "Bearer ${spotifyTokenManager.authenticationToken}"

	fun getMetadataByTrackArtistAndName(artist: String, name: String, album: String?): MetadataResponseDTO? {
		println(spotifyTokenManager.authenticationToken)

		val url = createSpotifyUrl(artist, name, 1)
		val result = restTemplate.searchSpotify(url)

		// Assume spotify has good relevance on its search and just grab the first result
		// (we already limited ourselves to 1 in the query parameter anyway)
		return result.tracks.items.firstOrNull()?.toMetadataResponseDTO()
	}

	fun RestTemplate.searchSpotify(url: String): SpotifySearchResponse {
		val headers = mapOf("Authorization" to authHeader).toHeaders()

		println(headers)

		val rawResponse = try {
			this.exchange(url, HttpMethod.GET, HttpEntity<String>(headers), String::class.java).body!!
		} catch (e: Exception) {
			logger.error("Failed to search spotify at URL: $url")

			throw e
		}

		return try {
			objectMapper.readValue(rawResponse, SpotifySearchResponse::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize Spotify search response! $rawResponse")

			throw e
		}
	}

	fun getLatestSongsByArtist(artist: String): List<MetadataResponseDTO> {
		val startingUrl = createSpotifyUrl(artist, null, REQUEST_SIZE_LIMIT)
		val allTracks = mutableListOf<SpotifyTrack>()

		var result = restTemplate.searchSpotify(startingUrl)
		allTracks.addAll(result.tracks.items)

		while (result.tracks.next != null) {
			// Spotify returns us a URL encoded string that it can't even handle... so decode it first
			val nextUrl = result.tracks.next!!.urlDecode()
			result = restTemplate.searchSpotify(nextUrl)
			allTracks.addAll(result.tracks.items)
		}

		return allTracks.map { it.toMetadataResponseDTO() }
	}

	private fun createSpotifyUrl(artist: String, name: String?, limit: Int): String {
		val artistQuery = "artist:$artist"
		val trackQuery = name?.let { "track:$name" }

		val query = artistQuery + if (trackQuery == null) "" else " $trackQuery"

		return URIBuilder()
				.setScheme("https")
				.setHost("api.spotify.com")
				.setPath("v1/search")
				.addParameter("limit", limit.toString())
				.addParameter("type", "track")
				.addParameter("q", "\"$query\"")
				.toUnencodedString()
	}

	data class SpotifySearchResponse(val tracks: SpotifySearchItems)
	data class SpotifySearchItems(val items: List<SpotifyTrack>, val next: String?)
	data class SpotifyArtist(val name: String)
	data class SpotifyAlbumImage(val height: Int, val width: Int, val url: String)

	data class SpotifyAlbum(
			val name: String,
			val images: List<SpotifyAlbumImage>,

			// Could be 1981-12-15, or 1981-12, or 1981. Doesn't matter. Year always first
			@JsonProperty("release_date")
			val releaseDate: String
	) {
		val releaseYear: Int = releaseDate.split("-").first().toInt()
	}

	data class SpotifyTrack(
			@JsonProperty("duration_ms")
			val durationMs: Long,

			@JsonProperty("track_number")
			val trackNumber: Int,

			val artists: List<SpotifyArtist>,
			val album: SpotifyAlbum,
			val href: String,
			val id: String,
			val name: String
	)

	private fun SpotifyTrack.toMetadataResponseDTO(): MetadataResponseDTO {
		val biggestImageUrl = this.album.images.maxBy { it.height }!!.url
		val biggestImage = ImageIO.read(URL(biggestImageUrl))

		return MetadataResponseDTO(
				name = this.name,
				artist = this.artists.first().name,
				album = this.album.name,
				releaseYear = this.album.releaseYear,
				trackNumber = this.trackNumber,
				albumArt = biggestImage,
				songLength = (this.durationMs / 1000).toInt()
		)
	}

	companion object {
		private val logger = logger()
	}
}

private class SpotifyTokenManager(private val restTemplate: RestTemplate) {
	// Spotify, annoyingly, requires that we continually refresh the token that we are using.
	// So we might not always have a valid token and might need to get ourselves another.
	// Check for the property in a synchronized block and request a new token if the last one is gone.
	private var currentAuthenticationToken: String? = null
		// Getter is essentially "non-null" but not sure how to tell Kotlin this without it being mad in some way.
		// I instead created a second property that asserts that it is not-null
		get() = synchronized(this) {
			return field ?: run {
				val newToken = getNewAuthenticationToken()
				currentAuthenticationToken = newToken

				newToken
			}
		}

	val authenticationToken: String
		get() = currentAuthenticationToken!!


	private fun getNewAuthenticationToken(): String {
		logger.info("Requesting new Spotify authentication token")
		val url = createSpotifyUrl()

		val rawResponse: String = try {
			val body = mapOf(
					"grant_type" to "client_credentials",
					"client_id" to CLIENT_ID,
					"client_secret" to CLIENT_SECRET
			).toPostBody()

			restTemplate.postForEntity(url, body, String::class.java).body!!
		} catch (e: Exception) {
			logger.error("Failed to Authenticate with Spotify with URL: $url")

			throw e
		}

		val parsedResponse: SpotifyAuthenticationResponse = try {
			objectMapper.readValue(rawResponse, SpotifyAuthenticationResponse::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize YoutubeApiClient search response! $rawResponse")

			throw e
		}

		// Expire the token thirty seconds earlier than it's actually set to expire for race condition safety
		val expireTokenMs = (parsedResponse.expiresIn - 30L) * 1000

		// Expire our token (because Spotify is going to anyway) so that the next request generate a new one
		Timer().schedule(expireTokenMs) {
			logger.info("Expiring old Spotify authentication token")
			currentAuthenticationToken = null
		}

		logger.info("Finished fetching new Spotify authentication token")
		return parsedResponse.accessToken
	}

	private fun createSpotifyUrl(): String {
		return URIBuilder()
				.setScheme("https")
				.setHost("accounts.spotify.com")
				.setPath("api/token")
				.toString()
	}

	data class SpotifyAuthenticationResponse(
			@JsonProperty("access_token")
			val accessToken: String,

			@JsonProperty("expires_in")
			val expiresIn: Int
	)

	companion object {
		private const val CLIENT_ID = "ea1ac3eb15084af1bd1f044a332d57ee"
		private const val CLIENT_SECRET = "22c82116884844959a3e6968b865eb25"

		val logger = logger()
	}
}
