package com.example.groove.controllers

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.enums.PermissionType
import com.example.groove.services.UserService
import com.example.groove.services.review.ReviewQueueService
import com.example.groove.services.review.ReviewSourceYoutubeChannelService
import com.example.groove.services.review.ReviewSourceArtistService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/review-queue")
class ReviewQueueController(
		private val reviewQueueService: ReviewQueueService,
		private val reviewSourceYoutubeChannelService: ReviewSourceYoutubeChannelService,
		private val reviewSourceArtistService: ReviewSourceArtistService,
		private val userService: UserService
) {

	@GetMapping
	fun getAllQueueSources(): List<ReviewSource> {
		return reviewQueueService.getAllQueueSourcesForCurrentUser()
	}

	@DeleteMapping("/{sourceId}")
	fun deleteQueueSource(@PathVariable("sourceId") sourceId: Long) {
		reviewQueueService.deleteReviewSource(sourceId)
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

		require(regex.matches(channelUrl)) {
			"Invalid channel URL supplied! $channelUrl"
		}

		val searchTerm = channelUrl.split("/").last()

		if (channelUrl.contains("channel", ignoreCase = true)) {
			reviewSourceYoutubeChannelService.subscribeToChannelId(searchTerm)
		} else {
			reviewSourceYoutubeChannelService.subscribeToUser(searchTerm)
		}
	}

	@PostMapping("/subscribe/artist")
	fun subscribeToArtist(@RequestBody body: ArtistSubscriptionDTO): ResponseEntity<Map<String, List<String>>> {
		require(body.artistName.isNotBlank()) {
			"Artist name must not be empty!"
		}

		val (success, possibleMatches) = reviewSourceArtistService.subscribeToArtist(body.artistName.trim())

		return if (success) {
			ResponseEntity.ok(emptyMap())
		} else {
			ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.body(mapOf("possibleMatches" to possibleMatches))
		}
	}

	@PostMapping("/check-new-songs")
	fun checkNewSongs() {
		userService.assertPermission(loadLoggedInUser(), PermissionType.RUN_REVIEW_QUEUES)

		logger.info("Running download jobs for all artist jobs...")
		reviewSourceArtistService.downloadNewSongs()

		logger.info("Running download jobs for all YouTube channel jobs...")
//		reviewSourceYoutubeChannelService.downloadNewSongs()
	}

	data class TrackRecommendDTO(
			val targetUserId: Long,
			val trackIds: List<Long>
	)

	data class YouTubeChannelSubscriptionDTO(val channelUrl: String)
	data class ArtistSubscriptionDTO(val artistName: String)

	companion object {
		val logger = logger()
	}
}
