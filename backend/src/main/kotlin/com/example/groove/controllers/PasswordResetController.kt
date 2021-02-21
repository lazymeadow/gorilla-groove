package com.example.groove.controllers

import com.example.groove.services.PasswordResetService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/password-reset")
class PasswordResetController(
		private val passwordResetService: PasswordResetService
) {

	@PostMapping
	fun initiatePasswordReset(@RequestBody body: PasswordResetDTO) {
		logger.info("A password reset is being initiated for ${body.email}")
		passwordResetService.initiatePasswordResetForUser(body.email)
	}

	@GetMapping("/unique-key/{uniqueKey}")
	fun getPasswordReset(@PathVariable uniqueKey: String) {
		passwordResetService.findPasswordReset(loadLoggedInUser(), uniqueKey)
	}

	@PutMapping
	fun changePassword(@RequestBody body: PasswordChangeDTO) {
		passwordResetService.changePassword(
				user = loadLoggedInUser(),
				newPassword = body.newPassword,
				uniqueKey = body.passwordResetUniqueKey
		)
	}

	companion object {
		private val logger = logger()
	}

	data class PasswordChangeDTO(
			val newPassword: String,
			val passwordResetUniqueKey: String
	)

	data class PasswordResetDTO(
			val email: String
	)
}
