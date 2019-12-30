package com.example.groove.db.dao

import com.example.groove.db.model.Device
import com.example.groove.db.model.Track
import com.example.groove.db.model.TrackHistory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface TrackHistoryRepository : CrudRepository<TrackHistory, Long> {
	@Query("""
			SELECT th
			FROM TrackHistory th
			JOIN FETCH th.track
			LEFT JOIN FETCH th.device
			WHERE th.deleted IS FALSE
			    AND (:userId IS NULL OR th.track.user.id = :userId)
			    AND (:loadPrivate IS TRUE OR th.track.private = FALSE)
				AND th.createdAt > :startDate
				AND th.createdAt < :endDate
			""")
	fun findAllByUserAndTimeRange(
			@Param("userId") userId: Long?,
			@Param("loadPrivate") loadPrivate: Boolean,
			@Param("startDate") startDate: Timestamp,
			@Param("endDate") endDate: Timestamp
	): List<TrackHistory>

	@Query("""
			SELECT th
			FROM TrackHistory th
			WHERE th.deleted IS FALSE
			AND th.track = :track
			ORDER BY th.createdAt DESC
			""")
	fun findMostRecentHistoryForTrack(
			@Param("track") track: Track,
			pageable: Pageable
	): List<TrackHistory>

	@Modifying
	@Query("""
			UPDATE TrackHistory th
			SET th.device = :newDevice
			WHERE th.device = :oldDevice
			""")
	fun updateDevice(
			@Param("newDevice") newDevice: Device,
			@Param("oldDevice") oldDevice: Device
	)
}
