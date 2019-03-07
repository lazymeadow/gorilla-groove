package com.example.groove.db.dao

import com.example.groove.db.model.TrackLink
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface TrackLinkRepository : CrudRepository<TrackLink, Long> {
	@Query("""
			SELECT tl
			FROM TrackLink tl
			WHERE tl.track.id = :trackId
			AND now() < tl.expiresAt
			""")
	fun findUnexpiredByTrackId(
			@Param("trackId") trackId: Long?
	): TrackLink?
}
