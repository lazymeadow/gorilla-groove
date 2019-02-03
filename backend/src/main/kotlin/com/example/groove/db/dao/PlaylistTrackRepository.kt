package com.example.groove.db.dao

import com.example.groove.db.model.Playlist
import com.example.groove.db.model.PlaylistTrack
import com.example.groove.db.model.Track
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PlaylistTrackRepository : JpaRepository<PlaylistTrack, Long> {
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

}
