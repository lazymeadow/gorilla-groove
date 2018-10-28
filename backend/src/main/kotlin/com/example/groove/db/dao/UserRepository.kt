package com.example.groove.db.dao

import com.example.groove.db.model.User
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long> {
	fun findByEmail(email: String): User?
}
