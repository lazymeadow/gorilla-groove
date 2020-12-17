package com.example.groove.db.dao

import com.example.groove.db.model.Playlist
import com.example.groove.db.model.PlaylistTrack
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface PlaylistTrackRepository : JpaRepository<PlaylistTrack, Long>, RemoteSyncableDao {
	@Query("SELECT pt " +
			"FROM PlaylistTrack pt " +
			"WHERE pt.playlist = :playlist " +
			"AND pt.track.deleted = FALSE " +
			"AND (:name IS NULL OR pt.track.name LIKE %:name%) " +
			"AND (:artist IS NULL OR pt.track.artist LIKE %:artist%) " +
			"AND (:album IS NULL OR pt.track.album LIKE %:album%)" +
			"AND (:searchTerm IS NULL " + // searchTerm is an 'OR' where the other terms are all ANDed
			"      OR pt.track.name LIKE %:searchTerm%" +
			"      OR pt.track.artist LIKE %:searchTerm%" +
			"      OR pt.track.featuring LIKE %:searchTerm%" +
			"      OR pt.track.album LIKE %:searchTerm%" +
			"      OR pt.track.genre LIKE %:searchTerm%" +
			"      OR pt.track.note LIKE %:searchTerm%)"
	)
	fun getTracks(
			@Param("name") name: String? = null,
			@Param("artist") artist: String? = null,
			@Param("album") album: String? = null,
			@Param("playlist") playlist: Playlist,
			@Param("searchTerm") searchTerm: String? = null,
			pageable: Pageable = Pageable.unpaged()
	): Page<PlaylistTrack>

		@Query("""
			SELECT pt
			FROM PlaylistTrack pt
			WHERE pt.updatedAt > :minimum
			AND pt.updatedAt < :maximum
			AND (pt.deleted = FALSE OR pt.createdAt < :minimum)
			AND pt.playlist.id IN (
			  SELECT pu.playlist.id
			  FROM PlaylistUser pu
			  WHERE pu.user.id = :userId
			)
			ORDER BY pt.id
			""")
	// Filter out things that were created AND deleted in the same time frame
	// Order by ID so the pagination is predictable
	fun getPlaylistTracksUpdatedBetweenTimestamp(
				@Param("userId") userId: Long,
				@Param("minimum") minimum: Timestamp,
				@Param("maximum") maximum: Timestamp,
				pageable: Pageable
	): Page<PlaylistTrack>

	@Query("""
			SELECT max(pt.updatedAt)
			FROM PlaylistTrack pt
			WHERE pt.playlist.id IN (
			  SELECT pu.playlist.id
			  FROM PlaylistUser pu
			  WHERE pu.user.id = :userId
			)
			""")
	override fun getLastModifiedRow(userId: Long): Timestamp
}
