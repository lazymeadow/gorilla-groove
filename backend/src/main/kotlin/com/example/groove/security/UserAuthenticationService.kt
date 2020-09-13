package com.example.groove.security

import com.example.groove.controllers.UserAuthenticationDTO
import com.example.groove.db.model.User

interface UserAuthenticationService {
	fun login(authDTO: UserAuthenticationDTO, ipAddress: String?): String

	fun logout(user: User)

	fun findByToken(token: String): User
}
