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
	fun logout(@RequestBody userLogoutDTO: UserLogoutDTO): ResponseEntity<String> {
		userAuthenticationService.logout(loadLoggedInUser(), userLogoutDTO.token)
		return ResponseEntity.ok().build()
	}

	data class UserLogoutDTO(
			val token: String
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
