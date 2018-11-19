package com.example.groove.controllers

import com.example.groove.security.UserAuthenticationService
import com.example.groove.util.loadLoggedInUser

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("authentication")
class AuthenticationController @Autowired constructor(
		private val userAuthenticationService: UserAuthenticationService
) {

	@PostMapping("/login")
	fun login(@RequestBody userAuthenticationDTO: UserAuthenticationDTO): ResponseEntity<AuthResponseDTO> {
		val token = userAuthenticationService.login(userAuthenticationDTO.email, userAuthenticationDTO.password)
		return ResponseEntity
				.ok(AuthResponseDTO(token, userAuthenticationDTO.email))
	}

	@PostMapping("/logout")
	fun logout(@RequestBody userLogoutDTO: AuthResponseDTO): ResponseEntity<String> {
		userAuthenticationService.logout(loadLoggedInUser(), userLogoutDTO.token)
		return ResponseEntity.ok().build()
	}

	data class AuthResponseDTO(
			val token: String,
			val email: String
	)

	data class UserAuthenticationDTO(
			val email: String,
			val password: String
	)

}
