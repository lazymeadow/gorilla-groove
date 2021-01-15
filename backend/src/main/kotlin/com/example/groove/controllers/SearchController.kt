package com.example.groove.controllers

import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.services.SpotifyApiClient
import com.example.groove.services.YoutubeApiClient
import com.example.groove.services.YoutubeDownloadService
import com.example.groove.util.loadLoggedInUser
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

	// This is currently used by clients to try to match a spotify song to a video on youtube
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
	): MetadataSearchResponse {
		logger.info("User ${loadLoggedInUser().name} is searching spotify for the artist '$artist'")
		return MetadataSearchResponse(
				items = spotifyApiClient.getTracksForArtistSortedByYear(artist = artist)
		)
	}

	// Do I still use this? I'm not sure I still use this
	@GetMapping("/spotify/artist/{artist}/name/{name}")
	fun searchSpotifyByArtistAndName(
			@PathVariable("artist") artist: String,
			@PathVariable("name") name: String
	): List<MetadataResponseDTO> {
		logger.info("User ${loadLoggedInUser().name} is searching spotify for the artist '$artist' and name '$name'")
		return spotifyApiClient.getMetadataByTrackArtistAndName(artist = artist, name = name)
	}

	// Used to help suggest items to users while they are filling in forms
	@GetMapping("/autocomplete/spotify/artist-name/{partial-name}")
	fun autocompleteSpotifyArtistName(
			@PathVariable("partial-name") partialName: String
	): AutocompleteResponse {
		logger.info("User ${loadLoggedInUser().name} is querying spotify autocomplete data for the term '$partialName'")
		return AutocompleteResponse(
				// I'm not sure if these artist names are unique or not. Seems like they probably can't be, right?
				// So put them into a Set
				suggestions = spotifyApiClient.searchArtistsByName(partialName, limit = 10).map { it.name }.toSet()
		)
	}

	@GetMapping("/autocomplete/youtube/channel-name/{partial-name}")
	fun autocompleteYoutubeChannelName(
			@PathVariable("partial-name") partialName: String
	): AutocompleteResponse {
		logger.info("User ${loadLoggedInUser().name} is querying youtube autocomplete data for the term '$partialName'")
		return AutocompleteResponse(
				// Linked so it preserves order- order is the YouTube given relevance and we don't want to discard it.
				// However, channel titles are not unique and there is no benefit to returning 7 of the same name
				suggestions = LinkedHashSet(youtubeApiClient.findChannels(partialName).map { it.channelTitle })
		)
	}

	companion object {
		private val logger = logger()
	}

	data class AutocompleteResponse(val suggestions: Set<String>)
	data class MetadataSearchResponse(val items: List<MetadataResponseDTO>)
}
