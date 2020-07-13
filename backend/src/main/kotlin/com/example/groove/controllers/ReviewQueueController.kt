package com.example.groove.controllers

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.Track
import com.example.groove.services.ReviewQueueService
import com.example.groove.services.ReviewSourceYoutubeChannelService
import com.example.groove.services.TrackService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/review-queue")
class ReviewQueueController(
		private val reviewQueueService: ReviewQueueService,
		private val trackService: TrackService,
		private val reviewSourceYoutubeChannelService: ReviewSourceYoutubeChannelService
) {

	@GetMapping("/track")
	fun getAllTracks(
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<Track> {
		return reviewQueueService.getTracksInReviewForCurrentUser(pageable)
	}

	@GetMapping("/source")
	fun getAllQueueSources(): List<ReviewSource> {
		return reviewQueueService.getAllQueueSourcesForCurrentUser()
	}

	@PostMapping("/recommend")
	fun recommend(@RequestBody body: TrackRecommendDTO) {
		reviewQueueService.recommend(body.targetUserId, body.trackIds)
	}

	@PostMapping("/subscribe/youtube-channel")
	fun subscribeToYoutubeChannel(@RequestBody body: YouTubeChannelSubscriptionDTO) {
		val channelUrl = body.channelUrl

		// Channel URL should conform to one of two patterns,
		// https://www.youtube.com/channel/UCSXm6c-n6lsjtyjvdD0bFVw
		// https://www.youtube.com/user/Liquicity

		val regex = Regex("^https://www.youtube.com/(channel|user)/.+\$")

		require (regex.matches(channelUrl)) {
			"Invalid channel URL supplied! $channelUrl"
		}

		val searchTerm = channelUrl.split("/").last()

		if (channelUrl.contains("channel", ignoreCase = true)) {
			reviewSourceYoutubeChannelService.subscribeToChannelId(searchTerm)
		} else {
			reviewSourceYoutubeChannelService.subscribeToUser(searchTerm)
		}
	}

	@PostMapping("/track/{trackId}/skip")
	fun skipTrack(@PathVariable("trackId") trackId: Long) {
		reviewQueueService.skipTrack(trackId)
	}

	@PostMapping("/track/{trackId}/approve")
	fun changeTrack(@PathVariable("trackId") trackId: Long) {
		reviewQueueService.addToLibrary(trackId)
	}

	@DeleteMapping("/track/{trackId}")
	fun deleteTrack(@PathVariable("trackId") trackId: Long) {
		trackService.deleteTracks(loadLoggedInUser(), listOf(trackId))
	}

	data class TrackRecommendDTO(
			val targetUserId: Long,
			val trackIds: List<Long>
	)

	data class YouTubeChannelSubscriptionDTO(val channelUrl: String)

	companion object {
		val logger = logger()
	}
}
