package com.example.groove.controllers

import com.example.groove.db.model.User
import com.example.groove.services.UserService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

	@GetMapping
	fun getUsers(): List<UserResponseDTO> {
		return userService.getAllUsers().map {
			UserResponseDTO(it.id, it.name, it.email)
		}
	}

	data class UserCreateDTO(
			val username: String,
			val email: String,
			val password: String
	)

	data class UserResponseDTO(
			val id: Long,
			val username: String,
			val email: String
	)
}
