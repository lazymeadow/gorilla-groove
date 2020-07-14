package com.example.groove.db.dao

import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.User
import org.springframework.data.repository.CrudRepository

interface ReviewSourceRepository : CrudRepository<ReviewSource, Long> {
	fun findBySubscribedUsers(user: User): List<ReviewSource>
}
