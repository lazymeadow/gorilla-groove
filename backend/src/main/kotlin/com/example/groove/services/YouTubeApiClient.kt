package com.example.groove.services

import com.example.groove.properties.YouTubeApiProperties
import com.example.groove.util.createMapper
import com.example.groove.util.logger
import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.sql.Timestamp
import java.time.Duration

@Service
class YoutubeApiClient(
		private val youTubeApiProperties: YouTubeApiProperties,
		private val restTemplate: RestTemplate
) {

	private val objectMapper = createMapper()

	fun findVideos(searchTerm: String): YoutubeApiResponse {
		// YouTube doesn't return all the necessary data in its 'search' response. So we search and collect the IDS,
		// then do a follow-up request with those IDs to get all the data we care about, while preserving video order.
		val videoIds = searchYoutube(searchTerm)
		val videoInfo = getVideoInformation(videoIds)

		return YoutubeApiResponse(
				videos = videoInfo.filter {
					it.snippet.liveBroadcastContent == LiveBroadcastContent.NONE
				}.map { YoutubeVideo(
						title = it.snippet.title,
						description = it.snippet.description,
						channelTitle = it.snippet.channelTitle,
						thumbnails = it.snippet.thumbnails,
						publishedAt = it.snippet.publishedAt,
						duration = it.contentDetails.duration.decodeIso8601(),
						viewCount = it.statistics.viewCount,
						likes = it.statistics.likeCount,
						dislikes = it.statistics.dislikeCount,
						videoUrl = VIDEO_URL + it.id,
						embedUrl = EMBED_URL + it.id,
						id = it.id
				) }.take(6) // Take only the top 6. The extras we query for are used for providing better results
		)
	}

	// Return a list instead of a set, because the order we get them in is Youtube's relevance
	private fun searchYoutube(searchTerm: String): List<String> {
		val url = "$SEARCH_API_URL&q=$searchTerm".withApiKey()

		val rawResponse = try {
			restTemplate.getForObject(url, String::class.java)!!
		} catch (e: Exception) {
			logger.error("Failed to search YouTube with URL: $url")

			throw e
		}

		val parsedResponse: RawYoutubeSearchResponse = try {
			objectMapper.readValue(rawResponse, RawYoutubeSearchResponse::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize YoutubeApiClient search response! $rawResponse")

			throw e
		}

		return parsedResponse.items.map { it.id.videoId }
	}

	private fun getVideoInformation(videoIds: List<String>): List<RawYoutubeVideoInfoItem> {
		if (videoIds.isEmpty()) {
			return emptyList()
		}

		val idString = videoIds.joinToString(",")
		val url = "$LIST_VIDEOS_URL&id=$idString".withApiKey()

		val rawResponse = try {
			restTemplate.getForObject(url, String::class.java)!!
		} catch (e: Exception) {
			logger.error("Failed to get YouTube video information with URL: $url")

			throw e
		}

		val parsedResponse = try {
			objectMapper.readValue(rawResponse, RawYoutubeVideoInfoResponse::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize YoutubeApiClient video info response! $rawResponse")

			throw e
		}

		return parsedResponse.items
	}

	// Comes back in ISO 8601 format, which looks something like "PT1H38M7S"
	fun String.decodeIso8601(): Long {
		val duration = Duration.parse(this)
		return duration.seconds
	}

	fun String.withApiKey(): String {
		return this + "&key=${youTubeApiProperties.youtubeApiKey}"
	}

	data class YoutubeApiResponse(
			val videos: List<YoutubeVideo>
	)

	data class YoutubeVideo(
			val title: String,
			val description: String,
			val channelTitle: String,
			val thumbnails: ItemThumbnails,
			val duration: Long,
			val viewCount: Long,
			val likes: Long,
			val dislikes: Long,
			val videoUrl: String,
			val embedUrl: String,
			val publishedAt: Timestamp,
			val id: String
	)

	data class RawYoutubeSearchResponse(
			val nextPageToken: String?, // Unused right now, but if we ever want to page through results we'll need it
			val items: List<RawYoutubeSearchItem>
	)

	data class RawYoutubeSearchItem(
			val id: ItemId
	)

	data class RawYoutubeVideoInfoResponse(
			val nextPageToken: String?, // Unused right now, but if we ever want to page through results we'll need it
			val items: List<RawYoutubeVideoInfoItem>
	)

	data class RawYoutubeVideoInfoItem(
			val id: String,
			val snippet: ItemSnippet,
			val contentDetails: ItemContentDetails,
			val statistics: ItemStatistics
	)

	data class ItemId(val videoId: String)

	// Doesn't seem to be any value for "this content was live but now isn't". That's just considered "none"
	@Suppress("unused")
	enum class LiveBroadcastContent {
		NONE, LIVE;

		@JsonValue // Api gives us values "none" and "live" in lowercase which are not beautiful Kotlin enum names
		fun apiValue() = this.name.toLowerCase()
	}

	data class ItemSnippet(
			val title: String,
			val description: String,
			val channelTitle: String,
			val thumbnails: ItemThumbnails,
			val liveBroadcastContent: LiveBroadcastContent,
			val publishedAt: Timestamp
	)

	data class ItemThumbnails(
			val default: ItemThumbnail,
			val medium: ItemThumbnail,
			val high: ItemThumbnail,
			val maxres: ItemThumbnail?
	)

	data class ItemThumbnail(
			val url: String,
			val width: Int,
			val height: Int
	)

	data class ItemContentDetails(val duration: String)

	data class ItemStatistics(
			val viewCount: Long,
			val likeCount: Long,
			val dislikeCount: Long
	)

	companion object {
		private const val API_BASE = "https://www.googleapis.com/youtube/v3/"

		const val SEARCH_API_URL = "${API_BASE}search?part=id&type=video&maxResults=10"
		const val LIST_VIDEOS_URL = "${API_BASE}videos?part=snippet,contentDetails,statistics&maxResults=10"

		const val VIDEO_URL = "https://www.youtube.com/watch?v="
		const val EMBED_URL = "https://www.youtube.com/embed/"

		val logger = logger()
	}
}