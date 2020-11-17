package com.example.groove.controllers

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.services.SpotifyApiClient
import com.example.groove.services.YoutubeApiClient
import com.example.groove.services.YoutubeDownloadService
import com.example.groove.util.logger

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/search")
class SearchController(
		private val youtubeApiClient: YoutubeApiClient,
		private val spotifyApiClient: SpotifyApiClient,
		private val youtubeDownloadService: YoutubeDownloadService
) {
	@GetMapping("/youtube/term/{term}")
	fun searchYoutubeForTerm(
			@PathVariable("term") term: String
	): YoutubeApiClient.YoutubeApiResponse {
		return youtubeApiClient.findVideos(term, limit = 6)
	}

	@GetMapping("/youtube/term/{term}/length/{length}")
	fun findVideoForTermAndLength(
			@PathVariable term: String,
			@PathVariable length: Int
	): YoutubeDownloadService.VideoProperties? {
		val properties = youtubeDownloadService.searchYouTube(searchTerm = term, targetLength = length).firstOrNull()
		if (properties == null) {
			logger.info("Could not find a matching video for search term $term")
		}
		// Youtube-DL is currently unreliable, so I've commented out the API client's implementation here as well
//		return youtubeApiClient.findVideos(searchTerm = term)
//				.videos
//				.firstOrNull { it.duration > length - 4 && it.duration < length + 4 }
		return properties
	}

	@GetMapping("/spotify/artist/{artist}")
	fun searchSpotifyByArtist(
			@PathVariable("artist") artist: String
	): List<MetadataResponseDTO> {
		return spotifyApiClient.getMetadataByTrackArtistAndName(artist = artist, name = null, limit = SPOTIFY_TRACK_LIMIT)
	}

	@GetMapping("/spotify/artist/{artist}/name/{name}")
	fun searchSpotifyByArtistAndName(
			@PathVariable("artist") artist: String,
			@PathVariable("name") name: String
	): List<MetadataResponseDTO> {
		return spotifyApiClient.getMetadataByTrackArtistAndName(artist = artist, name = name, limit = SPOTIFY_TRACK_LIMIT)
	}

	companion object {
		val logger = logger()

		private const val SPOTIFY_TRACK_LIMIT = 50
	}
}
