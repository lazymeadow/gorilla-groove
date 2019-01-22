package com.example.groove.controllers

import com.example.groove.db.model.User
import com.example.groove.services.UserService

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.Size

@RestController
@RequestMapping("api/user")
class UserController(
		private val userService: UserService
) {

	@PostMapping
	fun createUser(@Valid @RequestBody userCreateDTO: UserCreateDTO): ResponseEntity<String> {
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
			@field:Size(max = 32, message = "Username can be no longer than 32 characters")
			val username: String,

			@field:Email
			val email: String,

			@field:Size(min = 10, max = 255, message = "Password must be between 10 and 255 characters long")
			val password: String
	)

	data class UserResponseDTO(
			val id: Long,
			val username: String,
			val email: String
	)
}
