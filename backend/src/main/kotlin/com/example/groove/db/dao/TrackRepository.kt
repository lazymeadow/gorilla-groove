package com.example.groove.db.dao

import com.example.groove.db.model.Track
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface TrackRepository : CrudRepository<Track, Long> {
	@Query("SELECT t " +
			"FROM Track t " +
			"WHERE t.user.id = :userId " +
			"AND t.deleted = FALSE " +
			"AND (:loadHidden IS TRUE OR t.hidden = FALSE) " +
			"AND (:name IS NULL OR t.name LIKE %:name%) " +
			"AND (:artist IS NULL OR t.artist LIKE %:artist%) " +
			"AND (:album IS NULL OR t.album LIKE %:album%)" +
			"AND (:searchTerm IS NULL " + // searchTerm is an 'OR' where the other terms are all ANDed
			"      OR t.name LIKE %:searchTerm%" +
			"      OR t.artist LIKE %:searchTerm%" +
			"      OR t.featuring LIKE %:searchTerm%" +
			"      OR t.album LIKE %:searchTerm%" +
			"      OR t.genre LIKE %:searchTerm%" +
			"      OR t.note LIKE %:searchTerm%)"
	)
	fun getTracks(
			@Param("name") name: String? = null,
			@Param("artist") artist: String? = null,
			@Param("album") album: String? = null,
			@Param("userId") userId: Long,
			@Param("loadHidden") loadHidden: Boolean = false,
			@Param("searchTerm") searchTerm: String? = null,
			pageable: Pageable = Pageable.unpaged()
	): Page<Track>

	@Modifying
	@Query("UPDATE Track t " +
			"SET t.hidden = :isHidden " +
			"WHERE t.user.id = :userId " + // This user check is to prevent people hiding / showing
			"AND t.id IN :trackIds"  // other users' songs by guessing at IDs
	)
	fun setHiddenForUser(
			@Param("trackIds") trackIds: List<Long>,
			@Param("userId") userId: Long,
			@Param("isHidden") isHidden: Boolean
	)

	@Query("SELECT t " +
			"FROM Track t " +
			"WHERE t.fileName = :fileName " +
			"AND t.deleted = FALSE"
	)
	fun findAllByFileName(
			@Param("fileName") fileName: String
	): List<Track>
}
