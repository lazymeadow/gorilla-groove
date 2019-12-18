package com.example.groove.services

import com.example.groove.db.dao.*
import com.example.groove.db.model.*
import com.example.groove.db.model.enums.DeviceType
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class VersionService(
		private val versionHistoryRepository: VersionHistoryRepository
) {
	@Transactional(readOnly = true)
	fun getVersionHistories(deviceType: DeviceType, limit: Int): List<VersionHistory> {
		return versionHistoryRepository.findMostRecentVersionHistories(
				deviceType = deviceType,
				pageable = PageRequest.of(0, limit)
		)
	}

	companion object {
		val logger = LoggerFactory.getLogger(VersionService::class.java)!!
	}
}
