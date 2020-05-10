package com.example.groove.services

import com.example.groove.db.dao.ReviewSourceUserRecommendRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.ReviewSourceUserRecommend
import com.example.groove.db.model.Track
import com.example.groove.util.get
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ReviewQueueService(
		private val trackRepository: TrackRepository,
		private val userRepository: UserRepository,
		private val reviewSourceUserRecommendRepository: ReviewSourceUserRecommendRepository,
		private val trackService: TrackService
) {
	fun recommend(targetUserId: Long, recommendedTrackId: Long) {
		val targetUser = userRepository.get(targetUserId)
				?: throw IllegalArgumentException("No user found with ID $targetUserId!")

		val track = trackRepository.get(recommendedTrackId)

		val currentUser = loadLoggedInUser()

		if (track == null || track.private) {
			if (track?.private == true) {
				logger.error("User ${currentUser.name} tried to recommend the private track ${track.id}!")
			}
			throw IllegalArgumentException("No track found with ID: ${track?.id}!")
		}

		val reviewSource = reviewSourceUserRecommendRepository.findByUser(currentUser)
				?: ReviewSourceUserRecommend(user = currentUser).also { reviewSourceUserRecommendRepository.save(it) }

		trackService.saveTrackForUserReview(targetUser, track, reviewSource)
	}

	fun getAllForCurrentUser(pageable: Pageable): Page<Track> {
		return trackRepository.getTracksInReview(loadLoggedInUser().id, pageable)
	}

	companion object {
		val logger = logger()
	}
}
