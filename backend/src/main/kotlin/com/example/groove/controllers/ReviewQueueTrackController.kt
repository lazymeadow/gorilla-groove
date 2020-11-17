package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.services.review.ReviewQueueService
import com.example.groove.services.TrackService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/review-queue/track")
class ReviewQueueTrackController(
		private val reviewQueueService: ReviewQueueService,
		private val trackService: TrackService
) {

	@GetMapping
	fun getAllTracks(
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<ReviewTrackResponse> {
		return reviewQueueService.getTracksInReviewForCurrentUser(null, pageable).map {
			it.toReviewTrackResponse()
		}
	}

	@GetMapping("/review-source-id/{reviewSourceId}")
	fun getAllTracksWithSourceId(
			@PathVariable("reviewSourceId") reviewSourceId: Long,
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<ReviewTrackResponse> {
		return reviewQueueService.getTracksInReviewForCurrentUser(reviewSourceId, pageable).map {
			it.toReviewTrackResponse()
		}
	}

	private fun Track.toReviewTrackResponse() = ReviewTrackResponse(
			id = this.id,
			name = this.name,
			artist = this.artist,
			album = this.album,
			length = this.length,
			reviewSourceId = this.reviewSource!!.id
	)

	@PostMapping("/{trackId}/skip")
	fun skipTrack(@PathVariable("trackId") trackId: Long) {
		reviewQueueService.skipTrack(trackId)
	}

	@PostMapping("/{trackId}/approve")
	fun changeTrack(@PathVariable("trackId") trackId: Long) {
		reviewQueueService.addToLibrary(trackId)
	}

	@DeleteMapping("/{trackId}")
	fun deleteTrack(@PathVariable("trackId") trackId: Long) {
		trackService.deleteTracks(loadLoggedInUser(), listOf(trackId))
	}

	data class ReviewTrackResponse(
			val id: Long,
			val name: String,
			val artist: String,
			val album: String,
			val length: Int,
			val reviewSourceId: Long
	)

	companion object {
		val logger = logger()
	}
}
