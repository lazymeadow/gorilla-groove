package com.example.groove.controllers

import com.example.groove.security.UserAuthenticationService
import com.example.groove.services.UserService
import com.example.groove.util.loadLoggedInUser

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/authentication")
class AuthenticationController(
		private val userAuthenticationService: UserAuthenticationService,
		private val userService: UserService
) {

	@PostMapping("/login")
	fun login(@RequestBody userAuthenticationDTO: UserAuthenticationDTO): ResponseEntity<AuthResponseDTO> {
		val token = userAuthenticationService.login(userAuthenticationDTO.email, userAuthenticationDTO.password)
		val user = userService.getUserByEmail(userAuthenticationDTO.email)!!
		return ResponseEntity
				.ok(AuthResponseDTO(token, userAuthenticationDTO.email, user.name))
	}

	@PostMapping("/logout")
	fun logout(
			@CookieValue("cookieToken") cookieToken: String?,
			@RequestBody userLogoutDTO: UserLogoutDTO
	): ResponseEntity<String> {
		val token = when {
			cookieToken != null -> cookieToken
			userLogoutDTO.token != null -> userLogoutDTO.token
			else -> throw IllegalArgumentException("No valid token found to log out with")
		}

		userAuthenticationService.logout(loadLoggedInUser(), token)
		return ResponseEntity.ok().build()
	}

	@Deprecated("Use the cookie token instead. Leaving this around for Android compatibility")
	data class UserLogoutDTO(
			val token: String?
	)

	data class AuthResponseDTO(
			val token: String,
			val email: String,
			val username: String
	)

	data class UserAuthenticationDTO(
			val email: String,
			val password: String
	)

}
