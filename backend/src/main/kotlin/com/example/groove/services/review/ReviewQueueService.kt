package com.example.groove.services.review

import com.example.groove.db.dao.ReviewSourceRepository
import com.example.groove.db.dao.ReviewSourceUserRecommendRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.ReviewSourceUserRecommend
import com.example.groove.db.model.Track
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
		private val trackService: TrackService,
		private val reviewSourceRepository: ReviewSourceRepository,
		private val reviewQueueSocketHandler: ReviewQueueSocketHandler
) {
	@Transactional
	fun recommend(targetUserId: Long, recommendedTrackIds: List<Long>) {
		val currentUser = loadLoggedInUser()

		if (currentUser.id == targetUserId) {
			throw IllegalArgumentException("Don't recommend songs to yourself!")
		}
		val targetUser = userRepository.get(targetUserId)
				?: throw IllegalArgumentException("No user found with ID $targetUserId!")

		logger.info("User ${currentUser.name} is recommending ${recommendedTrackIds.size} tracks to ${targetUser.name}")

		val tracks = recommendedTrackIds.map { trackRepository.get(it) }

		// Validate everything before we bother to start inserting
		tracks.forEach { track ->
			if (track == null || track.user.id != currentUser.id) {
				throw IllegalArgumentException("No track found with ID: ${track?.id}!")
			}

			trackRepository.getTracksForUserWithOriginalTrack(targetUserId, track.id)
					// If the track is inReview, then block it always. That way you can't re-recommend
					// a track someone has already rejected. Otherwise, if the track isn't in review,
					// check if it's been deleted. If it hasn't, then block as they already copied it
					.firstOrNull { it.inReview || !it.deleted }
					?.let {
						logger.info("User ${currentUser.name} tried to recommend the song ${track.artist} - ${track.name} to ${targetUser.name} but it already existed")
						throw IllegalArgumentException("${track.name} already exists in this user's library or was recommended before!")
					}
		}

		val reviewSource = reviewSourceUserRecommendRepository.findByUser(currentUser)
				?: ReviewSourceUserRecommend(user = currentUser).also { reviewSourceUserRecommendRepository.save(it) }

		if (!reviewSource.isUserSubscribed(targetUser)) {
			reviewSource.subscribedUsers.add(targetUser)
			reviewSourceUserRecommendRepository.save(reviewSource)
		}

		tracks.forEach { track ->
			trackService.saveTrackForUserReview(targetUser, track!!, reviewSource, setAsCopied = true)
		}
		if (tracks.isNotEmpty()) {
			reviewQueueSocketHandler.broadcastNewReviewQueueContent(targetUserId, reviewSource, tracks.size)
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
		track.addedToLibrary = now()
		track.lastReviewed = track.addedToLibrary

		trackRepository.save(track)
	}

	@Transactional
	fun skipTrack(trackId: Long) {
		val track = trackRepository.get(trackId)
		track.assertValidReviewTrack(trackId)

		track!!.lastReviewed = now()
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

		val sources =  reviewSourceRepository.findBySubscribedUsers(user)
		val sourceIds = sources.map { it.id }
		val test = trackRepository.getTrackCountsForReviewSources(user.id, sourceIds)

		val sourceIdToCount = test.map { it.first() to it.last() }.toMap()

		return sources.map { ReviewSourceWithCount(it, sourceIdToCount[it.id] ?: 0) }
	}

	@Transactional
	fun deleteReviewSource(sourceId: Long) {
		val user = loadLoggedInUser()
		val existingSource = reviewSourceRepository.get(sourceId)
				?: throw IllegalArgumentException("No review source with ID $sourceId found")

		val existingIndex = existingSource.subscribedUsers.indexOfFirst { it.id == user.id }
		if (existingIndex == -1) {
			throw IllegalArgumentException("No review source with ID $sourceId found")
		}

		existingSource.subscribedUsers.removeAt(existingIndex)
		if (existingSource.subscribedUsers.isEmpty()) {
			existingSource.deleted = true
			existingSource.updatedAt = now()
		}

		reviewSourceRepository.save(existingSource)

		trackRepository.deleteTracksInReviewForSource(userId = user.id, reviewSourceId = existingSource.id)
	}

	companion object {
		val logger = logger()
	}
}

class ReviewSourceWithCount(
		val reviewSource: ReviewSource,

		@Suppress("unused")
		val trackCount: Long
)
