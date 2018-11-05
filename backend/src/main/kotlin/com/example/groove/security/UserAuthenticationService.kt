package com.example.groove.security

import com.example.groove.db.model.User

interface UserAuthenticationService {
	fun login(email: String, password: String): String

	fun logout(user: User, token: String)

	fun findByToken(token: String): User
}
