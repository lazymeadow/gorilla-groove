package com.example.groove.controllers

import com.example.groove.db.model.enums.DeviceType
import com.example.groove.security.UserAuthenticationService
import com.example.groove.services.UserService
import com.example.groove.util.isUuid
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("api/authentication")
class AuthenticationController(
		private val userAuthenticationService: UserAuthenticationService,
		private val userService: UserService
) {

	@PostMapping("/login")
	fun login(
			@RequestBody userAuthenticationDTO: UserAuthenticationDTO,
			request: HttpServletRequest
	): ResponseEntity<AuthResponseDTO> {
		val ipAddress = request.getHeader("x-forwarded-for")

		val token = userAuthenticationService.login(userAuthenticationDTO, ipAddress)
		val user = userService.getUserByEmail(userAuthenticationDTO.email)!!

		logger.info(userAuthenticationDTO.email + " logged in")

		userAuthenticationDTO.deviceId?.let {
			require(it.isUuid()) { "Client device ID must be in a UUID format!" }
		}

		return ResponseEntity.ok(AuthResponseDTO(
				id = user.id,
				token = token,
				email = userAuthenticationDTO.email,
				username = user.name
		))
	}

	@PostMapping("/logout")
	fun logout(): ResponseEntity<String> {
		userAuthenticationService.logout(loadLoggedInUser())
		return ResponseEntity.ok().build()
	}

	data class AuthResponseDTO(
			val id: Long,
			val token: String,
			val email: String,
			val username: String
	)

	companion object {
		val logger = logger()
	}
}

data class UserAuthenticationDTO(
		val email: String,
		val password: String,
		val deviceId: String?, // This and other device stuff SHOULD be mandatory going forward. But all clients need to update
		val preferredDeviceName: String?, // iPhones (and probably Android?) apps can read the phone's name. This is a better default than LOTR names
		val version: String?,
		val deviceType: DeviceType?
)
