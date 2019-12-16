package com.example.groove.services

import com.example.groove.db.dao.TrackHistoryRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.TrackHistory
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp


@Service
class TrackHistoryService(
		private val trackHistoryRepository: TrackHistoryRepository,
		private val userRepository: UserRepository
) {

	@Transactional(readOnly = true)
	fun getTrackHistory(userId: Long?, startDate: Timestamp, endDate: Timestamp): List<TrackHistory> {
		val currentUser = loadLoggedInUser()
		val targetUser = if (userId != null) { userRepository.findById(userId).unwrap() } else { null }

		val loadPrivate = targetUser != null && targetUser.id == currentUser.id

		return trackHistoryRepository.findAllByUserAndTimeRange(targetUser?.id, loadPrivate, startDate, endDate)
	}

	companion object {
		val logger = LoggerFactory.getLogger(TrackHistoryService::class.java)!!
	}
}
