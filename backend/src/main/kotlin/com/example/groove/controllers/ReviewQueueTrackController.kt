package com.example.groove.controllers

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
		return reviewQueueService.getTracksInReviewForCurrentUser(pageable).map {
			ReviewTrackResponse(
					id = it.id,
					name = it.name,
					artist = it.artist,
					album = it.album,
					length = it.length,
					reviewSourceId = it.reviewSource!!.id
			)
		}
	}

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
