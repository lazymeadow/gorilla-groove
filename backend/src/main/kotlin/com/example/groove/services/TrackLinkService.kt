package com.example.groove.services

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class TrackLinkService(private val trackLinkRepository: TrackLinkRepository) {

	@Transactional
	@Scheduled(cron = "0 0 5 * * *") // 5 AM every day (UTC)
	fun deleteExpiredTrackLinks() {
		logger.info("Deleting expired track links ...")

		trackLinkRepository.deleteExpiredTrackLinks()

		logger.info("Finished deleting expired track links")
	}

	companion object {
		val logger = logger()
	}
}
