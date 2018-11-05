package com.example.groove.db.dao

import com.example.groove.db.model.UserToken
import org.springframework.data.repository.CrudRepository

interface UserTokenRepository : CrudRepository<UserToken, Long> {
	fun findByToken(token: String): UserToken?
	fun deleteByToken(token: String)
}
