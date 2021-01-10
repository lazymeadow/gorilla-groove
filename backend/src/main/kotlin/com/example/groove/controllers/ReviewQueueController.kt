package com.example.groove.controllers

import com.example.groove.db.model.ReviewSourceUserDTO
import com.example.groove.db.model.enums.PermissionType
import com.example.groove.services.UserService
import com.example.groove.services.review.ReviewQueueService
import com.example.groove.services.review.ReviewSourceYoutubeChannelService
import com.example.groove.services.review.ReviewSourceArtistService
import com.example.groove.services.review.ReviewSourceWithCount
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*
import kotlin.IllegalArgumentException

@RestController
@RequestMapping("api/review-queue")
class ReviewQueueController(
		private val reviewQueueService: ReviewQueueService,
		private val reviewSourceYoutubeChannelService: ReviewSourceYoutubeChannelService,
		private val reviewSourceArtistService: ReviewSourceArtistService,
		private val userService: UserService
) {

	@GetMapping
	fun getAllQueueSources(): List<ReviewSourceWithCount> {
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
	fun subscribeToYoutubeChannel(@RequestBody body: YouTubeChannelSubscriptionDTO): ReviewSourceUserDTO {
		val channelUrl = body.channelUrl

		logger.info("${loadLoggedInUser().name} is subscribing to YT channel $channelUrl")

		val source = if (channelUrl != null) {
			// Channel URL should conform to one of two patterns,
			// https://www.youtube.com/channel/UCSXm6c-n6lsjtyjvdD0bFVw
			// https://www.youtube.com/user/Liquicity

			val regex = Regex("^https://www.youtube.com/(channel|user)/.+\$")

			require(regex.matches(channelUrl)) {
				"Invalid channel URL supplied! $channelUrl"
			}

			val identificationTerm = channelUrl.split("/").last()

			if (channelUrl.contains("channel", ignoreCase = true)) {
				reviewSourceYoutubeChannelService.subscribeToChannelId(identificationTerm)
			} else {
				reviewSourceYoutubeChannelService.subscribeToUser(identificationTerm)
			}
		} else if (body.channelTitle != null) {
			reviewSourceYoutubeChannelService.subscribeToChannelTitle(body.channelTitle)
		} else {
			throw IllegalArgumentException("Either a channelTitle or a channelUrl must be supplied")
		}

		return source.toSyncDTO()
	}

	// This is dumb but I want a different response body for Mobile than I do Web. Probably temporary (TM)
	@PostMapping("/subscribe/artist-web")
	fun subscribeToArtistFromWeb(@RequestBody body: ArtistSubscriptionDTO): ResponseEntity<Map<String, List<String>>> {
		logger.info("${loadLoggedInUser().name} is subscribing to artist ${body.artistName} from web")
		require(body.artistName.isNotBlank()) {
			"Artist name must not be empty!"
		}

		val (userSource, possibleMatches) = reviewSourceArtistService.subscribeToArtist(body.artistName.trim())

		return if (userSource != null) {
			ResponseEntity.ok(emptyMap())
		} else {
			ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.body(mapOf("possibleMatches" to possibleMatches))
		}
	}

	@PostMapping("/subscribe/artist")
	fun subscribeToArtistFromMobile(@RequestBody body: ArtistSubscriptionDTO): ReviewSourceUserDTO {
		logger.info("${loadLoggedInUser().name} is subscribing to artist ${body.artistName} from mobile")
		require(body.artistName.isNotBlank()) {
			"Artist name must not be empty!"
		}

		val (userSource, _) = reviewSourceArtistService.subscribeToArtist(body.artistName.trim())

		return userSource?.toSyncDTO() ?: throw IllegalArgumentException("No Artist found with name ${body.artistName}!")
	}

	@PostMapping("/check-new-songs")
	fun checkNewSongs() {
		userService.assertPermission(loadLoggedInUser(), PermissionType.RUN_REVIEW_QUEUES)

		logger.info("Running download jobs for all artist jobs...")
		reviewSourceArtistService.downloadNewSongs()

		logger.info("Running download jobs for all YouTube channel jobs...")
		reviewSourceYoutubeChannelService.downloadNewSongs()
	}

	data class TrackRecommendDTO(
			val targetUserId: Long,
			val trackIds: List<Long>
	)

	data class YouTubeChannelSubscriptionDTO(
			val channelUrl: String?,
			val channelTitle: String? // This is the user-facing name. It is NOT unique
	)
	data class ArtistSubscriptionDTO(val artistName: String)

	companion object {
		private val logger = logger()
	}
}
