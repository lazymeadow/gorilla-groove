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
	@Query("SELECT pt.track " +
			"FROM PlaylistTrack pt " +
			"WHERE pt.playlist = :playlist " +
			"AND (:name IS NULL OR pt.track.name LIKE %:name%) " +
			"AND (:artist IS NULL OR pt.track.artist LIKE %:artist%) " +
			"AND (:album IS NULL OR pt.track.album LIKE %:album%)"
	)
	fun getTracks(
			@Param("name") name: String? = null,
			@Param("artist") artist: String? = null,
			@Param("album") album: String? = null,
			@Param("playlist") playlist: Playlist,
			pageable: Pageable = Pageable.unpaged()
	): Page<Track>

}
