package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSourceUserRecommend
import com.example.groove.db.model.User
import org.springframework.data.repository.CrudRepository

interface ReviewSourceUserRecommendRepository : CrudRepository<ReviewSourceUserRecommend, Long> {
	fun findByUser(user: User): ReviewSourceUserRecommend?
}
