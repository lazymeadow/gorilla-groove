package com.example.groove.controllers

import com.example.groove.db.model.User
import com.example.groove.services.UserService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("user")
class UserController @Autowired constructor(
		private val userService: UserService
) {

	@PostMapping
	fun createUser(@RequestBody userCreateDTO: UserCreateDTO): ResponseEntity<String> {
		val user = User(0, userCreateDTO.username, userCreateDTO.email, userCreateDTO.password)
		userService.saveUser(user)

		return ResponseEntity
				.ok()
				.build()
	}

	data class UserCreateDTO(
			val username: String,
			val email: String,
			val password: String
	)
}
