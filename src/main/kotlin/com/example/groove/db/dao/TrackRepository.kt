package com.example.groove.db.dao

import com.example.groove.db.model.Track
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface TrackRepository : CrudRepository<Track, Long> {

	@Query("SELECT t FROM Track t WHERE t.name = :name")
	fun findTracks(@Param("name") name: String): List<Track>
}
