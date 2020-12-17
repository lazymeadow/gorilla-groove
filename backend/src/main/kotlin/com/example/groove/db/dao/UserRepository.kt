package com.example.groove.db.dao

import com.example.groove.db.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.sql.Timestamp

interface UserRepository : CrudRepository<User, Long>, RemoteSyncableDao {
	fun findByEmail(email: String): User?

	@Query("""
			SELECT u
			FROM User u
			WHERE u.updatedAt > :minimum
			AND u.updatedAt < :maximum
			AND (u.deleted = FALSE OR u.createdAt < :minimum)
			ORDER BY u.id
			""")
	fun getUsersUpdatedBetweenTimestamp(
			@Param("minimum") minimum: Timestamp,
			@Param("maximum") maximum: Timestamp,
			pageable: Pageable
	): Page<User>

	@Query("""
			SELECT max(u.updatedAt)
			FROM User u
			""")
	override fun getLastModifiedRow(userId: Long): Timestamp
}
