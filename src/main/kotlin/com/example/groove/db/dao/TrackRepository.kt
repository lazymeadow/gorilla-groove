package com.example.groove.db.dao

import com.example.groove.db.model.Track
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface TrackRepository : CrudRepository<Track, Long> {

	@Query("SELECT t " +
			"FROM Track t " +
			"WHERE (:name IS NULL OR t.name LIKE %:name%) " +
			"AND (:artist IS NULL OR t.artist LIKE %:artist%)" +
			"AND (:album IS NULL OR t.album LIKE %:album%)"
	)
	fun findTracks(
			@Param("name") name: String?,
			@Param("artist") artist: String?,
			@Param("album") album: String?,
			pageable: Pageable
	): Page<Track>
}
