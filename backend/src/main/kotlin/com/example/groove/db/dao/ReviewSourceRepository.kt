package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSource
import org.springframework.data.repository.CrudRepository

interface ReviewSourceRepository : CrudRepository<ReviewSource, Long> {
	fun findByReviewSourceImplementationId(reviewSourceImplementation: Long): ReviewSource?
}
