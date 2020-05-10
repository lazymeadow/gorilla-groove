package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.enums.ReviewSourceType
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface ReviewSourceRepository : CrudRepository<ReviewSource, Long> {
//	@Query("""
//			SELECT rs
//			FROM ReviewSource rs
//			WHERE rs.reviewSourceType = :reviewSourceType
//			AND rs.reviewSourceImplementationId = :reviewSourceImplementationId
//			""")
//	fun findByUniqueProperties(
//			@Param("reviewSourceType") reviewSourceType: ReviewSourceType,
//			@Param("reviewSourceImplementationId") reviewSourceImplementationId: Long
//	): ReviewSource?
}
