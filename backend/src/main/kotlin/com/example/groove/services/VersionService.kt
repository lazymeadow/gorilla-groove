package com.example.groove.services

import com.example.groove.db.dao.*
import com.example.groove.db.model.*
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.util.logger
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

	@Transactional
	fun createVersionHistory(deviceType: DeviceType, version: String, notes: String): VersionHistory {
		val versionHistory = VersionHistory(version = version, notes = notes, deviceType = deviceType)

		return versionHistoryRepository.save(versionHistory)
	}

	companion object {
		val logger = logger()
	}
}
