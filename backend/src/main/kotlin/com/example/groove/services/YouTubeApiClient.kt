package com.example.groove.services

import com.example.groove.properties.YouTubeApiProperties
import com.example.groove.util.createMapper
import com.example.groove.util.logger
import com.example.groove.util.toUnencodedString
import com.fasterxml.jackson.annotation.JsonValue
import org.apache.http.client.utils.URIBuilder
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

	fun findVideos(searchTerm: String? = null, channelId: String? = null, limit: Int = 100): YoutubeApiResponse {
		// I noticed that youtube's API seems to have results screwed up by including an ampersand??
		// I searched for "amba shepherd - wide awake & dreaming" and very few results were relevant,
		// and the results were very different from searching YouTube on the web. Removing the ampersand
		// seemed to un-screw it up. So that's what I'm doing here until it doesn't work.
		val fixedSearchTerm = searchTerm?.replace("&", "")

		// YouTube doesn't return all the necessary data in its 'search' response. So we search and collect the IDS,
		// then do a follow-up request with those IDs to get all the data we care about, while preserving video order.
		val videoIds = searchVideos(fixedSearchTerm, channelId)
		val videoInfo = getVideoInformation(videoIds)

		val videos = videoInfo.filter {
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
		) }.take(limit)

		return YoutubeApiResponse(videos)
	}

	fun findChannels(searchTerm: String): List<ChannelSearchSnippet> {
		val fixedSearchTerm = searchTerm.replace("&", "")

		val url = createSearchUrl(
				route = "search",
				searchTerm = fixedSearchTerm,
				additionalParams = mapOf(
						"part" to "snippet",
						"type" to YoutubeSearchType.CHANNEL.apiName,
						"order" to "relevance"
				)
		)

		val response = searchUrl<RawChannelSearchResponse>(url)

		return response.items.map { it.snippet }
	}

	// Return a list instead of a set, because the order we get them in is Youtube's relevance
	private fun searchVideos(searchTerm: String? = null, channelId: String? = null): List<String> {
		val url = createSearchUrl(
				route = "search",
				searchTerm = searchTerm,
				channelId = channelId,
				additionalParams = mapOf(
						"part" to "id",
						"type" to YoutubeSearchType.VIDEO.apiName,
						"order" to if (searchTerm != null) "relevance" else "date"
				)
		)

		val response = searchUrl<RawYoutubeSearchResponse>(url)

		return response.items.map { it.id.videoId }
	}

	private fun getVideoInformation(videoIds: List<String>): List<RawYoutubeVideoInfoItem> {
		if (videoIds.isEmpty()) {
			return emptyList()
		}

		val idString = videoIds.joinToString(",")
		val url = createSearchUrl(
				"videos",
				null,
				null,
				mapOf("id" to idString, "part" to "snippet,contentDetails,statistics")
		)

		return searchUrl<RawYoutubeVideoInfoResponse>(url).items
	}

	private fun createSearchUrl(
			route: String,
			searchTerm: String? = null,
			channelId: String? = null,
			additionalParams: Map<String, String> = emptyMap()
	): String {
		val builder = URIBuilder()
				.setScheme("https")
				.setHost("www.googleapis.com")
				.setPath("youtube/v3/$route")
				.addParameter("maxResults", "10")
				.addParameter("key", youTubeApiProperties.youtubeApiKey)

		searchTerm?.let { builder.addParameter("q", searchTerm) }
		channelId?.let { builder.addParameter("channelId", channelId) }

		additionalParams.forEach { (key, value) -> builder.addParameter(key, value) }

		return builder.toUnencodedString()
	}

	// Comes back in ISO 8601 format, which looks something like "PT1H38M7S"
	fun String.decodeIso8601(): Long {
		val duration = Duration.parse(this)
		return duration.seconds
	}

	fun getChannelInfoByChannelId(channelId: String): YoutubeChannelInfo? {
		val url = "https://www.googleapis.com/youtube/v3/channels?part=snippet&id=$channelId&key=${youTubeApiProperties.youtubeApiKey}"

		return getChannelResponse(url)
	}

	fun getChannelInfoByUsername(name: String): YoutubeChannelInfo? {
		val url = "https://www.googleapis.com/youtube/v3/channels?part=snippet&forUsername=$name&key=${youTubeApiProperties.youtubeApiKey}"

		return getChannelResponse(url)
	}

	private fun getChannelResponse(url: String): YoutubeChannelInfo? {
		val response = searchUrl<RawYoutubeChannelResponse>(url)

		if (response.items.isEmpty()) {
			return null
		}

		val item = response.items.first()

		return YoutubeChannelInfo(
				title = item.snippet.title,
				id = item.id
		)
	}

	private inline fun<reified T> searchUrl(url: String): T {
		val rawResponse = try {
			restTemplate.getForObject(url, String::class.java)!!
		} catch (e: Exception) {
			logger.error("Failed to search YouTube with URL: $url")

			throw e
		}

		return try {
			objectMapper.readValue(rawResponse, T::class.java)
		} catch (e: Exception) {
			logger.error("Failed to deserialize YoutubeApiClient search response! $rawResponse")

			throw e
		}
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

	data class RawChannelSearchResponse(val items: List<RawYoutubeChannelSearchItem>)
	data class RawYoutubeChannelSearchItem(val snippet: ChannelSearchSnippet)
	// There is also a property called "title", though it seems identical to "channelTitle" in all my testing
	data class ChannelSearchSnippet(val channelTitle: String, val channelId: String)

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

	data class RawYoutubeChannelResponse(val items: List<RawYoutubeChannelInfoItem>)
	data class RawYoutubeChannelInfoItem(val id: String, val snippet: YoutubeChannelSnippet)
	data class YoutubeChannelSnippet(val title: String)

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
		const val VIDEO_URL = "https://www.youtube.com/watch?v="
		const val EMBED_URL = "https://www.youtube.com/embed/"

		val logger = logger()
	}
}

data class YoutubeChannelInfo(val title: String, val id: String)

private enum class YoutubeSearchType(val apiName: String) {
	VIDEO("video"),
	CHANNEL("channel")
}
