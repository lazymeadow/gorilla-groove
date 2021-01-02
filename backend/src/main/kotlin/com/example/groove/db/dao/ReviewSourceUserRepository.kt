package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSourceUser
import com.example.groove.db.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface ReviewSourceUserRepository : CrudRepository<ReviewSourceUser, Long>, RemoteSyncableDao {
	@Query("""
			SELECT rsu
			FROM ReviewSourceUser rsu
			WHERE rsu.reviewSource.id = :sourceId
			AND rsu.user.id = :userId
			""")
	fun findByUserAndSource(
			@Param("sourceId") sourceId: Long,
			@Param("userId") userId: Long
	): ReviewSourceUser?

	@Query("""
			SELECT rsu
			FROM ReviewSourceUser rsu
			WHERE rsu.user.id = :userId
			AND rsu.updatedAt > :minimum
			AND rsu.updatedAt < :maximum
			AND (rsu.deleted = FALSE OR rsu.createdAt < :minimum)
			ORDER BY rsu.id
			""")
	fun getReviewSourcesUpdatedBetweenTimestamp(
			@Param("userId") userId: Long,
			@Param("minimum") minimum: Timestamp,
			@Param("maximum") maximum: Timestamp,
			pageable: Pageable
	): Page<ReviewSourceUser>

	// It might make sense to do a JOIN FETCH here to speed up the review source fetching we do afterwards
	@Query("""
			SELECT rsu
			FROM ReviewSourceUser rsu
			WHERE rsu.user = :user
			AND rsu.deleted = FALSE
			""")
	fun findActiveByUser(
			@Param("user") user: User
	): List<ReviewSourceUser>

	@Query("""
			SELECT max(rsu.updatedAt)
			FROM ReviewSourceUser rsu
			WHERE rsu.id = :userId
			""")
	override fun getLastModifiedRow(userId: Long): Timestamp?}
