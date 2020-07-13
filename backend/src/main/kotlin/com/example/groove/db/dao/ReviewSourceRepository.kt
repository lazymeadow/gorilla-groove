package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.User
import com.example.groove.db.model.enums.ReviewSourceType
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface ReviewSourceRepository : CrudRepository<ReviewSource, Long> {
//	@Query("""
//		SELECT rs
//		FROM ReviewSource rs
//		JOIN User u
//		ON u.id = rs.user_id
//		WHERE u = :userId
//			""")
//	fun findByUniqueProperties(
//			@Param("reviewSourceType") reviewSourceType: ReviewSourceType,
//			@Param("reviewSourceImplementationId") reviewSourceImplementationId: Long
//	): ReviewSource?
//
	fun findBySubscribedUsers(user: User): List<ReviewSource>
}
