package com.example.groove.services

import com.example.groove.db.dao.TrackHistoryRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.TrackHistory
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.util.get
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.data.domain.PageRequest

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp


@Service
class TrackHistoryService(
		private val trackRepository: TrackRepository,
		private val trackHistoryRepository: TrackHistoryRepository,
		private val userRepository: UserRepository
) {

	@Transactional(readOnly = true)
	fun getTrackHistory(userId: Long?, startDate: Timestamp, endDate: Timestamp): List<TrackHistory> {
		val currentUser = loadLoggedInUser()
		val targetUser = userId?.let { userRepository.get(it) }

		val loadPrivate = targetUser != null && targetUser.id == currentUser.id

		return trackHistoryRepository.findAllByUserAndTimeRange(targetUser?.id, loadPrivate, startDate, endDate)
	}

	@Transactional
	fun deleteTrackHistory(id: Long) {
		val trackHistory = trackHistoryRepository.get(id)
		if (trackHistory == null || trackHistory.deleted || trackHistory.track.user.id != loadLoggedInUser().id) {
			throw ResourceNotFoundException("No track history found with ID $id")
		}

		trackHistory.deleted = true

		trackHistoryRepository.save(trackHistory)

		// It's entirely possible that this track update stuff gets out of sync if someone listens to the same song
		// while we are in the middle of doing this.... but it's real unlikely and it's nothing that can't be fixed
		val track = trackHistory.track
		val mostRecentHistory = trackHistoryRepository.findMostRecentHistoryForTrack(track, PageRequest.of(0, 1))

		track.lastPlayed = mostRecentHistory.firstOrNull()?.createdAt
		track.playCount = track.playCount - 1

		trackRepository.save(track)
	}

	companion object {
		val logger = logger()
	}
}
