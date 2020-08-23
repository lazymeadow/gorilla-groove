package com.example.groove.db.dao

import com.example.groove.db.model.TrackLink
import com.example.groove.services.ArtSize
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
			AND isArt = FALSE
			AND tl.audioFormat = :audioFormat
			AND now() < tl.expiresAt
			""")
	fun findUnexpiredSongByTrackIdAndAudioFormat(
			@Param("trackId") trackId: Long,
			@Param("audioFormat") audioFormat: AudioFormat
	): TrackLink?

	@Query("""
			SELECT tl
			FROM TrackLink tl
			WHERE tl.track.id = :trackId
			AND isArt = TRUE
			AND tl.artSize = :artSize
			AND now() < tl.expiresAt
			""")
	fun findUnexpiredArtByTrackIdAndArtSize(
			@Param("trackId") trackId: Long,
			@Param("artSize") artSize: ArtSize
	): TrackLink?

	@Modifying
	@Query("""
			UPDATE TrackLink tl
			SET tl.expiresAt = '1970-01-02 00:00:01.0'
			WHERE tl.track.id = :trackId
			""")
	fun forceExpireLinksByTrackId(
			@Param("trackId") trackId: Long
	)

	@Modifying
	@Query("""
		DELETE FROM TrackLink tl
		WHERE tl.expiresAt < now()
	""")
	fun deleteExpiredTrackLinks()
}
