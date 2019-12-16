package com.example.groove.db.dao

import com.example.groove.db.model.Track
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface TrackRepository : CrudRepository<Track, Long> {
	@Query("""
			SELECT t
			FROM Track t
			WHERE t.user.id = :userId
			AND t.deleted = FALSE
			AND (t.hidden IS FALSE OR :loadHidden IS TRUE)
			AND (:loadPrivate IS TRUE OR t.private = FALSE)
			AND (:name IS NULL OR t.name LIKE %:name%)
			AND (:artist IS NULL OR t.artist LIKE %:artist%)
			AND (:album IS NULL OR t.album LIKE %:album%)
			AND (:searchTerm IS NULL
				OR t.name LIKE %:searchTerm%
				OR t.artist LIKE %:searchTerm%
				OR t.featuring LIKE %:searchTerm%
				OR t.album LIKE %:searchTerm%
				OR t.genre LIKE %:searchTerm%
				OR t.note LIKE %:searchTerm%)
		""")
	fun getTracks(
			@Param("name") name: String? = null,
			@Param("artist") artist: String? = null,
			@Param("album") album: String? = null,
			@Param("userId") userId: Long,
			@Param("loadPrivate") loadPrivate: Boolean = false,
			@Param("loadHidden") loadHidden: Boolean = false,
			@Param("searchTerm") searchTerm: String? = null,
			pageable: Pageable = Pageable.unpaged()
	): Page<Track>

	@Modifying
	@Query("""
			UPDATE Track t
			SET t.private = :isPrivate, t.updatedAt = now()
			WHERE t.user.id = :userId
			AND t.id IN :trackIds
			""")
	fun setPrivateForUser(
			@Param("trackIds") trackIds: List<Long>,
			@Param("userId") userId: Long,
			@Param("isPrivate") isPrivate: Boolean
	)

	@Query("""
			SELECT t
			FROM Track t
			WHERE t.fileName = :fileName
			AND t.deleted = FALSE
			""")
	fun findAllByFileName(
			@Param("fileName") fileName: String
	): List<Track>

	@Query("""
			SELECT count(t)
			FROM Track t
			WHERE t.createdAt > :timestamp
			AND t.private = FALSE
			AND t.deleted = FALSE
			""")
	fun countAllTracksAddedSinceTimestamp(
			@Param("timestamp") timestamp: Timestamp
	): Int

	@Query("""
			SELECT t
			FROM Track t
			WHERE t.updatedAt > :timestamp
			AND t.user.id = :userId
			""")
	fun getTracksUpdatedSinceTimestamp(
			@Param("userId") userId: Long,
			@Param("timestamp") timestamp: Timestamp
	): List<Track>
}
