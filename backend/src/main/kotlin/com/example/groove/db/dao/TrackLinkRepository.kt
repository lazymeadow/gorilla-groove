package com.example.groove.db.dao

import com.example.groove.db.model.TrackLink
import com.example.groove.services.enums.AudioFormat
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface TrackLinkRepository : CrudRepository<TrackLink, Long> {
	@Query("""
			SELECT tl
			FROM TrackLink tl
			WHERE tl.track.id = :trackId
			AND (:audioFormat IS NULL OR tl.audioFormat = :audioFormat)
			AND now() < tl.expiresAt
			""")
	fun findUnexpiredByTrackIdAndAudioFormat(
			@Param("trackId") trackId: Long,
			@Param("audioFormat") audioFormat: AudioFormat?
	): TrackLink?

	@Modifying
	@Query("""
			UPDATE TrackLink tl
			SET tl.expiresAt = now()
			WHERE tl.track.id = :trackId
			""")
	fun forceExpireLinksByTrackId(
			@Param("trackId") trackId: Long
	)
}
