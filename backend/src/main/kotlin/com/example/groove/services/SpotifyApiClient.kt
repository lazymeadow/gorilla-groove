package com.example.groove.services

import com.example.groove.dto.MetadataDTO
import com.example.groove.properties.SpotifyApiProperties
import com.example.groove.util.*
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.http.client.utils.URIBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.lang.RuntimeException
import java.net.URI
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

	// Spotify doesn't actually let us query by year, but I think it makes the most sense for the frontend
	// to see things by year. So we "get around" this by querying multiple pages and sorting on our end. It won't
	// be a complete picture unfortunately. But it should be good enough for most artists that have fewer than ~200 songs
	fun getTracksForArtistSortedByYear(artist: String): List<MetadataDTO> {
		val allMetadata = mutableListOf<MetadataDTO>()
		for (i in 0..4) {
			val page = getMetadataByTrackArtistAndName(
					artist = artist,
					offset = i * REQUEST_SIZE_LIMIT,
					limit = REQUEST_SIZE_LIMIT,
					allowBroadSearch = false
			)

			if (page.isEmpty()) {
				break
			}
			allMetadata.addAll(page)
		}

		return allMetadata
				.asSequence()
				// Spotify will pretty aggressively add artists that aren't what you searched for.
				// e.g. if you search for LIONE you might get Lionezz or Lionel. Filter out the not-actual-matches
				.filter { result ->
					result.artist.split(",").any {
						individualArtist -> individualArtist.trim().equals(artist, ignoreCase = true)
					}
				}
				// Mixed versions are gross. Not real full songs. I am making the executive decision that nobody should see these
				.filterNot { result ->
					result.name.contains("(mixed)", ignoreCase = true) || result.name.contains("(mix cut)", ignoreCase = true)
				}
				// There are a crapload of remixes on spotify. I only think that remixes make sense to see if the
				// artist that you are interested in DID the remix. If the artist you are interested in had their
				// music remixed by somebody else, then it seems safe to exclude from the search
				.filter { result ->
					if (result.name.contains("remix", ignoreCase = true)) {
						// The best way I can tell to make sure our artist did the remix is to just make sure that our
						// artist's name shows up somewhere in the title since that's where Spotify seems to credit things most reliably
						result.name.contains(artist, ignoreCase = true)
					} else {
						true
					}
				}
				// Spotify can have duplicates apparently. No it isn't a result of paginating incorrectly as I originally
				// thought I might be doing. They have different "sourceIds". Everything else is the same. So exclude them
				// by putting them into a Set
				.toSet()
				.sortedWith(compareBy({ -it.releaseYear }, { it.album }, { it.trackNumber }))
				.toList()
	}

	fun getMetadataByTrackArtistAndName(
			artist: String,
			name: String? = null,
			offset: Int = 0,
			limit: Int = REQUEST_SIZE_LIMIT,
			allowBroadSearch: Boolean = true
	): List<MetadataDTO> {
		val url = createSpotifySearchUrl(
				artist = artist,
				name = name,
				offset = offset,
				limit = limit,
				searchType = "track"
		)
		val result = restTemplate.querySpotify<SpotifyTrackSearchResponse>(url)

		// Spotify searches don't agree with multi-artist searches a lot of the time. If this fails, there's a very
		// good chance that it could succeed if we only take one artist out, and search with them alone.
		if (result.tracks.items.isEmpty() && allowBroadSearch) {
			// These are the two most common ways of splitting up an artist- comma and ampersand
			artist.findIndex { it == ',' || it == '&' }?.let { characterIndex ->
				val firstArtist = artist.substring(0, characterIndex).trim()
				logger.info("Could not find metadata for artist: '$artist', and name: '$name'. Trying again with only one artist: '$firstArtist'")

				return getMetadataByTrackArtistAndName(artist = firstArtist, name = name, offset = offset, limit = limit, allowBroadSearch = false)
			}
		}

		// Assume spotify has good relevance on its search and just grab the first result
		// (we already limited ourselves to 1 in the query parameter anyway)
		return result.tracks.items.map { it.toMetadataResponseDTO() }
	}

	private inline fun<reified T> RestTemplate.querySpotify(url: URI): T {
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
	): List<MetadataDTO> {
		val artistIdNotNull = artistId // Save ourselves a call to spotify if we already know the ID
				?: getSpotifyArtistId(artist)
				?: return emptyList()

		// This particular function is concerned with making sure we get ALL tracks past a certain date. Because we can't
		// actually sort an artist's songs by date, we have to get creative and first find all albums, since there are a lot fewer.
		// Once we have the albums, we can see which ones are a new release, and ask spotify for tracks off of them to essentially filter by year.
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

	fun searchArtistsByName(artist: String, limit: Int = REQUEST_SIZE_LIMIT): List<SpotifyArtist> {
		val startingUrl = createSpotifySearchUrl(artist = artist, limit = limit, searchType = "artist")

		val result = restTemplate.querySpotify<SpotifyArtistSearchResponse>(startingUrl)

		return result.artists.items
	}

	private fun createSpotifySearchUrl(
			artist: String,
			name: String? = null,
			offset: Int = 0,
			limit: Int = REQUEST_SIZE_LIMIT,
			searchType: String
	): URI {
		val artistQuery = "artist:$artist"
		val trackQuery = name?.let { "track:$name" }

		val query = artistQuery + if (trackQuery == null) "" else " $trackQuery"

		val uri = URIBuilder()
				.setScheme("https")
				.setHost("api.spotify.com")
				.setPath("v1/search")
				.addParameter("offset", offset.toString())
				.addParameter("limit", limit.toString())
				.addParameter("type", searchType)
				.addParameter("q", query)
				.build()

		// This is pretty stupid, but because URIBuilder already encodes things (can't tell it not to apparently),
		// and RestTemplate ALSO wants to encode things, we get stuff that is double encoded. However, if we re-create
		// the URI using this "build(encoded = true)" function, then RestTemplate knows not to encode it again. Dumb.
		return UriComponentsBuilder.fromUri(uri).build(true).toUri()
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

	private fun SpotifyTrack.toMetadataResponseDTO(): MetadataDTO {
		val biggestImageUrl = this.album!!.images.maxByOrNull { it.height }!!.url

		return MetadataDTO(
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
		val url = URI("https://api.spotify.com/v1/artists/$artistId/albums")

		val result = restTemplate.querySpotify<SpotifyAlbumResponse>(url)

		return result.items.map { it.id }.toSet()
	}

	// The albums returned by this do have tracks included
	fun getTracksFromAlbumIds(artistId: String, artistName: String, albumIds: Set<String>): List<SpotifyTrack> {
		val url = URI("https://api.spotify.com/v1/albums?ids=${albumIds.joinToString(",")}")

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
			duplicateTracks.minByOrNull { it.album!!.releaseDate.toDate().toEpochDay() }!!
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
