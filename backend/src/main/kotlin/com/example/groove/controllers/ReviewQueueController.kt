package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.services.ReviewQueueService
import com.example.groove.util.logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/review-queue")
class ReviewQueueController(
		private val reviewQueueService: ReviewQueueService
) {

	@GetMapping
	fun getAll(
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<Track> {
		return reviewQueueService.getAllForCurrentUser(pageable)
	}

	@PostMapping("/recommend")
	fun recommend(@RequestBody body: TrackRecommendDTO) {
		reviewQueueService.recommend(body.targetUserId, body.trackId)
	}

	data class TrackRecommendDTO(
			val targetUserId: Long,
			val trackId: Long
	)

	companion object {
		val logger = logger()
	}
}
