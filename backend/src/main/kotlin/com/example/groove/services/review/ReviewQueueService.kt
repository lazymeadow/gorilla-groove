package com.example.groove.services.review

import com.example.groove.db.dao.*
import com.example.groove.db.model.*
import com.example.groove.services.TrackService
import com.example.groove.services.socket.ReviewQueueSocketHandler
import com.example.groove.util.DateUtils.now
import com.example.groove.util.get
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewQueueService(
		private val trackRepository: TrackRepository,
		private val userRepository: UserRepository,
		private val reviewSourceUserRecommendRepository: ReviewSourceUserRecommendRepository,
		private val reviewSourceUserRepository: ReviewSourceUserRepository,
		private val trackService: TrackService,
		private val reviewQueueSocketHandler: ReviewQueueSocketHandler
) {
	@Transactional
	fun recommend(targetUserIds: List<Long>, recommendedTrackIds: List<Long>) {
		val currentUser = loadLoggedInUser()

		if (targetUserIds.any { it == currentUser.id }) {
			throw IllegalArgumentException("Don't recommend songs to yourself!")
		}
		val targetUsers = targetUserIds.map { targetUserId ->
			userRepository.get(targetUserId) ?: throw IllegalArgumentException("No user found with ID $targetUserId!")
		}

		val userString = targetUsers.joinToString { it.name }
		logger.info("User ${currentUser.name} is recommending ${recommendedTrackIds.size} tracks to ($userString)")

		val tracks = recommendedTrackIds.map { trackRepository.get(it) }

		// Validate everything before we bother to start inserting
		tracks.forEach { track ->
			// TODO planning to make it so you can recommend someone else's music in the future.
			// Will replace this check with a private check instead
			if (track == null || track.user.id != currentUser.id) {
				throw IllegalArgumentException("No track found with ID: ${track?.id}!")
			}
		}

		val reviewSource = reviewSourceUserRecommendRepository.findByUser(currentUser)
				?: ReviewSourceUserRecommend(user = currentUser).also { reviewSourceUserRecommendRepository.save(it) }

		targetUsers.forEach { targetUser ->
			if (!reviewSource.isUserSubscribed(targetUser)) {
				val reviewSourceUser = ReviewSourceUser(reviewSource = reviewSource, user = targetUser)
				reviewSourceUserRepository.save(reviewSourceUser)
			}
		}

		val trackCountRecommendedPerUser = mutableMapOf<Long, Int>()

		tracks.forEach { track ->
			targetUsers.forEach { targetUser ->
				// Don't re-recommend something if they've already imported or rejected the song previously
				// I used to have this return an error to the caller, but now that this endpoint supports
				// multiple users and multiple tracks, it gets hairy to return a response and I don't want to deal with it
				val priorRecommendation = trackRepository.getTracksForUserWithOriginalTrack(
						userId = targetUser.id,
						originalTrackId = track!!.id
				).firstOrNull { it.inReview || !it.deleted }

				if (priorRecommendation == null) {
					trackService.saveTrackForUserReview(targetUser, track, reviewSource, setAsCopied = true)
					trackCountRecommendedPerUser[targetUser.id] = (trackCountRecommendedPerUser[targetUser.id] ?: 0) + 1
				} else {
					logger.info("User ${currentUser.name} tried to recommend the song ${track.artist} - ${track.name} to ${targetUser.name} but it already existed")
				}
			}
		}

		targetUserIds.forEach { targetUserId ->
			val count = trackCountRecommendedPerUser[targetUserId] ?: 0
			if (count > 0) {
				reviewQueueSocketHandler.broadcastNewReviewQueueContent(targetUserId, reviewSource, count)
			}
		}
	}

	@Transactional
	fun getTracksInReviewForCurrentUser(reviewSourceId: Long? = null, pageable: Pageable): Page<Track> {
		return trackRepository.getTracksInReview(loadLoggedInUser().id, reviewSourceId, pageable)
	}

	@Transactional
	fun addToLibrary(trackId: Long) {
		val track = trackRepository.get(trackId)
		track.assertValidReviewTrack(trackId)

		track!!.inReview = false

		val now = now()
		track.addedToLibrary = now
		track.updatedAt = now
		track.lastReviewed = now

		trackRepository.save(track)
	}

	@Transactional
	fun skipTrack(trackId: Long) {
		val track = trackRepository.get(trackId)
		track.assertValidReviewTrack(trackId)

		val now = now()
		track!!.lastReviewed = now
		track.updatedAt = now
		trackRepository.save(track)
	}

	private fun Track?.assertValidReviewTrack(trackId: Long) {
		val user = loadLoggedInUser()

		if (this == null || this.user.id != user.id) {
			throw IllegalArgumentException("No track found with ID $trackId!")
		}

		if (!this.inReview) {
			throw IllegalArgumentException("Track $trackId is not in review!")
		}
	}

	@Transactional
	fun getAllQueueSourcesForCurrentUser(): List<ReviewSourceWithCount> {
		val user = loadLoggedInUser()

		val userSourceAssociations = reviewSourceUserRepository.findActiveByUser(user)
		val sourceIds = userSourceAssociations.map { it.reviewSource.id }
		val test = trackRepository.getTrackCountsForReviewSources(user.id, sourceIds)

		val sourceIdToCount = test.map { it.first() to it.last() }.toMap()

		return userSourceAssociations.map { ReviewSourceWithCount(it.toSyncDTO(), sourceIdToCount[it.reviewSource.id] ?: 0) }
	}

	@Transactional
	fun deleteReviewSource(sourceId: Long) {
		val user = loadLoggedInUser()

		val existingAssociation = reviewSourceUserRepository.findByUserAndSource(sourceId = sourceId, userId = user.id)
				?: throw IllegalArgumentException("No review source found with ID $sourceId")

		existingAssociation.deleted = true
		existingAssociation.active = false
		existingAssociation.updatedAt = now()
		reviewSourceUserRepository.save(existingAssociation)

		trackRepository.deleteTracksInReviewForSource(userId = user.id, reviewSourceId = existingAssociation.reviewSource.id)
	}

	companion object {
		private val logger = logger()
	}
}

class ReviewSourceWithCount(
		val reviewSource: ReviewSourceUserDTO,

		@Suppress("unused")
		val trackCount: Long
)
