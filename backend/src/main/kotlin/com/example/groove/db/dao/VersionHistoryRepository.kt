package com.example.groove.db.dao

import com.example.groove.db.model.VersionHistory
import com.example.groove.db.model.enums.DeviceType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface VersionHistoryRepository : CrudRepository<VersionHistory, Long> {
	@Query("""
			SELECT vh
			FROM VersionHistory vh
			WHERE vh.deviceType = :deviceType
			ORDER BY vh.id DESC
		""")
	fun findMostRecentVersionHistories(
			@Param("deviceType") deviceType: DeviceType,
			pageable: Pageable
	): List<VersionHistory>
}
