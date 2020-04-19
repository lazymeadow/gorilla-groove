package com.example.groove.services

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.util.createMapper
import com.example.groove.util.logger
import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.awt.Image
import java.io.IOException
import java.net.URL
import java.sql.Timestamp
import javax.imageio.ImageIO

// 200 is the max they allow. For artists with a lot of songs this gives us the best chance of finding a match
const val SEARCH_LIMIT = 200

@Service
class ITunesMetadataService(private val restTemplate: RestTemplate) {
	private val objectMapper = createMapper()

	// TODO we can only search iTunes ~20 times a minute, so throttling ourselves in some way here would be good
	fun getMetadataByTrackArtistAndName(artist: String, name: String, album: String?): MetadataResponseDTO? {
		val searchName = name.toLowerCase().trim()
		val searchArtist = artist.toLowerCase().trim()
		val searchAlbum = album?.toLowerCase()?.trim()
		// Because we can't search iTunes for both terms at once, we have to grab everything from one term,
		// and then parse the other one ourselves. There are a lot of potential ways this could go wrong, so
		// for increased resiliency I have opted to try to search twice- first by artist and then by title
		val iTunesTrack = (
				matchByArtist(artist = searchArtist, name = searchName, album = searchAlbum)
						?: matchByName(artist = searchArtist, name = searchName, album = searchAlbum)
				) ?: run {
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
				.addParameter("limit", SEARCH_LIMIT.toString())
	}

	private fun matchByArtist(artist: String, name: String, album: String?): ITunesTrack? {
		val url = createBaseBuilder()
				.addParameter("attribute", "artistTerm")
				.addParameter("term", artist)
				.toString()

		// Apple gives us a weird attachment response and the automatic deserialization barfs on it if we
		// try to go directly to an ITunesResponse. So just take it as a string and call Jackson ourselves
		logger.info("Searching for metadata by artist: $artist - $name. Sending URL $url")

		return getITunesResponse(url)
				?.findBestMatch(true, name, album)
				?: run {
					logger.warn("Failed to match (by artist): $artist - $name")
					null
				}
	}

	private fun matchByName(artist: String, name: String, album: String?): ITunesTrack? {
		val url = createBaseBuilder()
				.addParameter("attribute", "songTerm")
				.addParameter("term", name)
				.toString()

		logger.info("Searching for metadata by name: $artist - $name. Sending URL $url")

		return getITunesResponse(url)
				?.findBestMatch(false, artist, album)
				?: run {
					logger.warn("Failed to match (by name): $artist - $name")
					null
				}
	}

	private fun List<ITunesTrack>?.findBestMatch(matchByName: Boolean, matchTerm: String, album: String?): ITunesTrack? {
		if (this == null) {
			return null
		}

		// The same song could show up multiple times on different albums. So find all possible matches
		// and then narrow them down more to get our best match
		val possibleMatches = this.filter {
			val searchTerm = (if (matchByName) it.trackName else it.artistName).toLowerCase()
			searchTerm == matchTerm
		}

		if (possibleMatches.isEmpty()) {
			return null
		}

		if (album == null) {
			return possibleMatches.minBy { it.releaseDate }
		}

		// Try to match album first by an exact match, then a contains match, and finally find the oldest album otherwise
		return possibleMatches.find { it.collectionName!!.toLowerCase().trim() == album }
				?: possibleMatches.find {
					val collectionName = it.collectionName!!.toLowerCase().trim()
					collectionName.contains(album) || album.contains(collectionName)
				}
				?: possibleMatches.minBy { it.releaseDate }
	}

	private fun getITunesResponse(url: String): List<ITunesTrack>? {
		val rawResponse = restTemplate.getForObject(url, String::class.java)!!
		val response = try {
			objectMapper.readValue(rawResponse, ITunesResponse::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize iTunes response", e)
			logger.error("Raw response: $rawResponse")
			return null
		}

		if (response.resultCount == SEARCH_LIMIT) {
			logger.warn("iTunes search count matched our limit. We could have missed results! For URL $url")
		}

		return response.results.filter { it.kind == "song" }
	}

	data class ITunesResponse(
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

	private fun MetadataResponseDTO.Companion.fromITunesTrack(iTunesTrack: ITunesTrack): MetadataResponseDTO? {
		val albumArt = readAlbumArtToImage(iTunesTrack.artworkUrl100)
				?: return null

		return MetadataResponseDTO(
				name = iTunesTrack.trackName,
				artist = iTunesTrack.artistName,
				album = iTunesTrack.collectionName!!, // Non-music videos should have this link
				genre = iTunesTrack.primaryGenreName,
				releaseYear = iTunesTrack.releaseDate.toLocalDateTime().year,
				trackNumber = iTunesTrack.trackNumber,
				albumArt = albumArt
		)
	}

	private fun readAlbumArtToImage(albumArtUrl: String): Image? {
		// Url ends like <UUID>/source/100x100bb.jpg
		// So far I haven't seen any that don't conform. So drop that part and replace it with 600x600bb.jpg
		return if (albumArtUrl.endsWith("/100x100bb.jpg")) {
			val biggerUrl = albumArtUrl.dropLast(13) + "600x600bb.jpg"
			try {
				ImageIO.read(URL(biggerUrl))
			} catch (e: IOException) {
				logger.error("Failed to read in larger image URL! Falling back to smaller one. Bigger URL: $biggerUrl")
				try {
					ImageIO.read(URL(albumArtUrl))
				} catch (e: IOException) {
					logger.error("Failed to read in smaller image URL too! Oh no.")
					null
				}
			}
		} else {
			logger.warn("URL for iTunes album art did not end with '/100x100bb.jpg! Using smaller art")
			try {
				ImageIO.read(URL(albumArtUrl))
			} catch (e: IOException) {
				logger.error("Failed to read in image URL!")
				null
			}
		}
	}

	companion object {
		val logger = logger()
	}
}
