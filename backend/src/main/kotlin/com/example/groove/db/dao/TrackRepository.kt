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
			AND t.inReview = FALSE
			AND (t.hidden IS FALSE OR :loadHidden IS TRUE)
			AND (:loadPrivate IS TRUE OR t.private = FALSE)
			AND (:name IS NULL OR t.name LIKE %:name%)
			AND (:artist IS NULL OR t.artist LIKE %:artist%)
			AND (:album IS NULL OR t.album LIKE %:album%)
			AND (COALESCE(:excludedTrackIds) IS NULL OR t.id NOT IN :excludedTrackIds)
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
			@Param("excludedTrackIds") excludedTrackIds: List<Long>? = null,
			pageable: Pageable = Pageable.unpaged()
	): Page<Track>

	@Query("""
			SELECT t
			FROM Track t
			WHERE t.user.id = :userId
			AND t.inReview = TRUE
			AND t.deleted = FALSE
			AND (:reviewSourceId IS NULL OR t.reviewSource.id = :reviewSourceId)
			ORDER BY t.lastReviewed ASC
		""")
	fun getTracksInReview(
			@Param("userId") userId: Long,
			@Param("reviewSourceId") reviewSourceId: Long? = null,
			pageable: Pageable = Pageable.unpaged()
	): Page<Track>

	@Modifying
	@Query("""
			UPDATE Track t
			SET t.deleted = TRUE, t.updatedAt = now()
			WHERE t.user.id = :userId
			AND t.inReview = TRUE
			AND t.reviewSource.id = :reviewSourceId
		""")
	fun deleteTracksInReviewForSource(
			@Param("userId") userId: Long,
			@Param("reviewSourceId") reviewSourceId: Long
	)

	@Modifying
	@Query("""
			UPDATE Track t
			SET t.updatedAt = now()
			WHERE t.user.id = :userId
			AND t.deleted = FALSE
		""")
	fun setUpdatedAtForActiveTracksAndUser(
			@Param("userId") userId: Long
	)

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
			AND t.inReview = FALSE
			""")
	fun countAllTracksAddedSinceTimestamp(
			@Param("timestamp") timestamp: Timestamp
	): Int

	@Query("""
			SELECT t
			FROM Track t
			WHERE t.user.id = :userId
			AND t.originalTrack.id = :originalTrackId
			""")
	fun getTracksForUserWithOriginalTrack(
			@Param("userId") userId: Long,
			@Param("originalTrackId") originalTrackId: Long
	): List<Track>

	@Query("""
			SELECT t
			FROM Track t
			WHERE t.updatedAt > :minimum
			AND t.updatedAt < :maximum
			AND t.user.id = :userId
			AND (t.deleted = FALSE OR t.createdAt < :minimum)
			ORDER BY t.id
			""")
	// Filter out things that were created AND deleted in the same time frame
	// Order by ID so the pagination is predictable
	fun getTracksUpdatedBetweenTimestamp(
			@Param("userId") userId: Long,
			@Param("minimum") minimum: Timestamp,
			@Param("maximum") maximum: Timestamp,
			pageable: Pageable
	): Page<Track>

	@Query("""
			SELECT t.reviewSource.id, count(t)
			FROM Track t
			WHERE t.user.id = :userId
			AND t.deleted = FALSE
			AND t.inReview = TRUE
			AND t.reviewSource.id IN :reviewSourceIds
			GROUP BY t.reviewSource.id
			""")
	fun getTrackCountsForReviewSources(
			@Param("userId") userId: Long,
			@Param("reviewSourceIds") reviewSourceIds: List<Long>
	): List<Array<Long>>
}
