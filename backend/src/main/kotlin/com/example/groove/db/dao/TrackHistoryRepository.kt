package com.example.groove.db.dao

import com.example.groove.db.model.TrackHistory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface TrackHistoryRepository : CrudRepository<TrackHistory, Long> {
	@Query("""
			SELECT th
			FROM TrackHistory th
			WHERE (:userId IS NULL OR th.track.user.id = :userId)
			    AND (:loadHidden IS TRUE OR th.track.hidden = FALSE)
			    AND th.track.deleted = FALSE
				AND th.createdAt > :startDate
				AND th.createdAt < :endDate
			""")
	fun findAllByUserAndTimeRange(
			@Param("userId") userId: Long?,
			@Param("loadHidden") loadHidden: Boolean,
			@Param("startDate") startDate: Timestamp,
			@Param("endDate") endDate: Timestamp
	): List<TrackHistory>
}
