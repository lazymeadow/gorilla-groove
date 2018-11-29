package com.example.groove.security


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

@Component
class TokenAuthenticationProvider(
		private val auth: UserAuthenticationService
) : AbstractUserDetailsAuthenticationProvider() {

	override fun additionalAuthenticationChecks(d: UserDetails, auth: UsernamePasswordAuthenticationToken) {
		// Nothing to do
	}

	override fun retrieveUser(username: String, authentication: UsernamePasswordAuthenticationToken): UserDetails {
		val token = authentication.credentials
		return auth.findByToken(token.toString())
	}
}