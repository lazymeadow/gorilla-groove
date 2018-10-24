package com.example.groove.controllers

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.dao.UserTokenRepository
import com.example.groove.db.model.UserToken
import com.example.groove.security.UserAuthenticationService
import com.example.groove.services.UserService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
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