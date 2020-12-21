package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface ReviewSourceRepository : CrudRepository<ReviewSource, Long>, RemoteSyncableDao {
	fun findBySubscribedUsers(user: User): List<ReviewSource>

	@Query("""
			SELECT rs
			FROM ReviewSource rs
			JOIN rs.subscribedUsers u
			WHERE u.id = :userId
			AND rs.updatedAt > :minimum
			AND rs.updatedAt < :maximum
			AND (rs.deleted = FALSE OR rs.createdAt < :minimum)
			ORDER BY rs.id
			""")
	fun getReviewSourcesUpdatedBetweenTimestamp(
			@Param("userId") userId: Long,
			@Param("minimum") minimum: Timestamp,
			@Param("maximum") maximum: Timestamp,
			pageable: Pageable
	): Page<ReviewSource>

	@Query("""
			SELECT max(rs.updatedAt)
			FROM ReviewSource rs
			JOIN rs.subscribedUsers u
			WHERE u.id = :userId
			""")
	override fun getLastModifiedRow(userId: Long): Timestamp?
}
