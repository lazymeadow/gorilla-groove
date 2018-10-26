package com.example.groove.controllers

import com.example.groove.security.UserAuthenticationService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("authentication")
class AuthenticationController @Autowired constructor(
		private val userAuthenticationService: UserAuthenticationService
) {

	@PostMapping("/login")
	fun login(@RequestBody userAuthenticationDTO: UserAuthenticationDTO): ResponseEntity<String> {
		val token = userAuthenticationService.login(userAuthenticationDTO.email, userAuthenticationDTO.password)
		return ResponseEntity.ok(token)
	}

	data class UserAuthenticationDTO(
			val email: String,
			val password: String
	)
}