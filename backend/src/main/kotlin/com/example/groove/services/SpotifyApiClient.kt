package com.example.groove.services

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.properties.SpotifyApiProperties
import com.example.groove.util.*
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.http.client.utils.URIBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.*
import kotlin.concurrent.schedule


private val objectMapper = createMapper()

// It's an error to request more than this
private const val REQUEST_SIZE_LIMIT = 49

@Service
class SpotifyApiClient(
		spotifyApiProperties: SpotifyApiProperties,
		private val restTemplate: RestTemplate
) {

	private val spotifyTokenManager: SpotifyTokenManager = SpotifyTokenManager(spotifyApiProperties, restTemplate)

	private val authHeader: String
		get() = "Bearer ${spotifyTokenManager.authenticationToken}"

	fun getMetadataByTrackArtistAndName(artist: String, name: String?, limit: Int): List<MetadataResponseDTO> {
		val url = createSpotifySearchUrl(artist, name, limit, "track")
		val result = restTemplate.querySpotify<SpotifyTrackSearchResponse>(url)

		// Assume spotify has good relevance on its search and just grab the first result
		// (we already limited ourselves to 1 in the query parameter anyway)
		return result.tracks.items.map { it.toMetadataResponseDTO() }
	}

	private inline fun<reified T> RestTemplate.querySpotify(url: String): T {
		val headers = mapOf("Authorization" to authHeader).toHeaders()

		// Can be nice to log this to play with the API in a rest client
//		println(headers)

		val rawResponse = try {
			this.exchange(url, HttpMethod.GET, HttpEntity<String>(headers), String::class.java).body!!
		} catch (e: Exception) {
			logger.error("Failed to search spotify at URL: $url")

			throw e
		}

		return try {
			objectMapper.readValue(rawResponse, T::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize Spotify search response! $rawResponse")

			throw e
		}
	}

	fun getSongsByArtist(
			artist: String,
			artistId: String? = null,
			fromDate: LocalDate? = null
	): List<MetadataResponseDTO> {
		val artistIdNotNull = artistId // Save ourselves a call to spotify if we already know the ID
				?: getSpotifyArtistId(artist)
				?: return emptyList()

		val albumIds = getAlbumsIdsByArtistId(artistIdNotNull)
		val allTracks = getTracksFromAlbumIds(artistIdNotNull, artist, albumIds)

		val filteredTracks = fromDate?.let { date ->
			allTracks.filter { it.album!!.releaseDate.toDate().toEpochDay() > date.toEpochDay() }
		} ?: allTracks

		return filteredTracks.map { it.toMetadataResponseDTO() }
	}

	private fun getSpotifyArtistId(artist: String): String? {
		val lowerName = artist.toLowerCase()
		val matchingArtist = searchArtistsByName(artist).find { it.name.toLowerCase() == lowerName }

		return matchingArtist?.id
	}

	fun searchArtistsByName(artist: String): List<SpotifyArtist> {
		val startingUrl = createSpotifySearchUrl(artist, null, REQUEST_SIZE_LIMIT, "artist")

		val result = restTemplate.querySpotify<SpotifyArtistSearchResponse>(startingUrl)

		return result.artists.items
	}

	private fun createSpotifySearchUrl(artist: String, name: String?, limit: Int, searchType: String): String {
		val artistQuery = "artist:$artist"
		val trackQuery = name?.let { "track:$name" }

		val query = artistQuery + if (trackQuery == null) "" else " $trackQuery"

		return URIBuilder()
				.setScheme("https")
				.setHost("api.spotify.com")
				.setPath("v1/search")
				.addParameter("limit", limit.toString())
				.addParameter("type", searchType)
				.addParameter("q", query)
				.toUnencodedString()
	}

	data class SpotifyArtistSearchResponse(val artists: SpotifySearchItems<SpotifyArtist>)
	data class SpotifySearchItems<T>(val items: List<T>, val next: String?)
	data class SpotifyArtist(val name: String, val id: String)
	data class SpotifyAlbumImage(val height: Int, val width: Int, val url: String)
	data class SpotifyAlbumResponse(val items: List<SpotifyAlbum>)
	data class SpotifyAlbumBulkResponse(val albums: List<SpotifyAlbum>)

	data class SpotifyTrackSearchResponse(val tracks: SpotifySearchItems<SpotifyTrack>)

	data class SpotifyAlbum(
			val name: String,
			val images: List<SpotifyAlbumImage>,
			val id: String,
			val tracks: SpotifyTrackResponse? = null, // Not returned on all responses that return albums

			// Could be 1981-12-15, or 1981-12, or 1981. Doesn't matter. Year always first
			@JsonProperty("release_date")
			val releaseDate: String
	) {
		val releaseYear: Int = releaseDate.split("-").first().toInt()
	}

	data class SpotifyTrackResponse(val items: List<SpotifyTrack>)

	data class SpotifyTrack(
			@JsonProperty("duration_ms")
			val durationMs: Long,

			@JsonProperty("track_number")
			val trackNumber: Int,

			val artists: List<SpotifyArtist>,
			var album: SpotifyAlbum? = null, // Not always here depending on the endpoint
			val href: String,

			@JsonProperty("preview_url")
			val previewUrl: String?,
			val id: String,
			val name: String
	)

	private fun SpotifyTrack.toMetadataResponseDTO(): MetadataResponseDTO {
		val biggestImageUrl = this.album!!.images.maxBy { it.height }!!.url

		return MetadataResponseDTO(
				sourceId = this.id,
				name = this.name,
				artist = this.artists.joinToString { it.name },
				album = this.album!!.name,
				releaseYear = this.album!!.releaseYear,
				trackNumber = this.trackNumber,
				albumArtLink = biggestImageUrl,
				length = (this.durationMs / 1000).toInt(),
				previewUrl = this.previewUrl
		)
	}

	// The albums returned by this do not have tracks included
	fun getAlbumsIdsByArtistId(artistId: String): Set<String> {
		val url = "https://api.spotify.com/v1/artists/$artistId/albums"

		val result = restTemplate.querySpotify<SpotifyAlbumResponse>(url)

		return result.items.map { it.id }.toSet()
	}

	// The albums returned by this do have tracks included
	fun getTracksFromAlbumIds(artistId: String, artistName: String, albumIds: Set<String>): List<SpotifyTrack> {
		val url = "https://api.spotify.com/v1/albums?ids=${albumIds.joinToString(",")}"

		val result = restTemplate.querySpotify<SpotifyAlbumBulkResponse>(url)

		val allTracks = result.albums.map { album ->
			val tracksForAlbum = album.tracks!!.items
			tracksForAlbum.forEach { it.album = album } // We need this album info later, so store it on the track
			tracksForAlbum
		}.flatten()

		// Our artist might be on an album, but not have contributed to every song on said album. So do an additional filter
		val tracksWithArtist = allTracks.filter { track -> track.artists.find { it.id == artistId } != null }

		val lowerArtist = artistName.toLowerCase()
		val nonRemixTracks = tracksWithArtist.filterNot { track ->
			val lowerName = track.name.toLowerCase()
			// Remove remixes that aren't remixes our artist made. This is probably error prone though
			lowerName.contains("remix") && !lowerName.contains(lowerArtist)
		}

		// We can get the same song multiple times from different albums. They have different track IDs however
		val nameToTracks = nonRemixTracks.groupBy { it.name }
		return nameToTracks.values.map { duplicateTracks ->
			duplicateTracks.minBy { it.album!!.releaseDate.toDate().toEpochDay() }!!
		}
	}

	private fun String.toDate(): LocalDate {
		val sections = this.split("-")
		return when (sections.size) {
			1 -> Year.parse(this).atMonth(1).atDay(1)
			2 -> YearMonth.parse(this).atDay(1)
			3 -> LocalDate.parse(this)
			else -> throw RuntimeException("Weird date encountered on track! $this")
		}
	}

	companion object {
		private val logger = logger()
	}
}

private class SpotifyTokenManager(
		private val spotifyApiProperties: SpotifyApiProperties,
		private val restTemplate: RestTemplate
) {
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
					"client_secret" to spotifyApiProperties.spotifyApiSecret
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

		val logger = logger()
	}
}
