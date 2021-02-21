package com.example.groove.services

import com.example.groove.db.dao.PasswordResetRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.PasswordReset
import com.example.groove.db.model.User
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.services.email.EmailSender
import com.example.groove.util.DateUtils
import com.example.groove.util.logger
import com.example.groove.util.minusWeeks
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service

private const val PASSWORD_RESET_KEY_TEMPLATE = "---key---"

@Service
class PasswordResetService(
		private val emailSender: EmailSender,
		private val passwordResetRepository: PasswordResetRepository,
		private val userRepository: UserRepository
) {
	fun initiatePasswordResetForUser(email: String) {
		val user = userRepository.findByEmail(email) ?: run {
			logger.warn("No user could be found with the email $email for a password reset")
			throw IllegalArgumentException("No user found with the email $email")
		}

		val passwordReset = PasswordReset(user = user)
		passwordResetRepository.save(passwordReset)

		emailSender.sendEmail(
				subject = "Password Reset",
				message = passwordResetEmailTemplate.replace(PASSWORD_RESET_KEY_TEMPLATE, passwordReset.uniqueKey),
				toEmail = user.email
		)
	}

	fun findPasswordReset(user: User, uniqueKey: String): PasswordReset {
		val passwordReset = passwordResetRepository.findByUniqueKey(uniqueKey)

		if (passwordReset == null || passwordReset.user.id != user.id) {
			throw ResourceNotFoundException("No password reset found with uniqueKey '$uniqueKey'")
		}

		val oneWeekAgo = DateUtils.now().minusWeeks(1)
		if (passwordReset.createdAt.before(oneWeekAgo)) {
			throw IllegalStateException("The password reset has expired")
		}

		return passwordReset
	}

	fun changePassword(user: User, uniqueKey: String, newPassword: String) {
		val passwordReset = findPasswordReset(user, uniqueKey)

		user.encryptedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
		userRepository.save(user)

		passwordResetRepository.delete(passwordReset)
	}

	companion object {
		private val logger = logger()
	}
}

val passwordResetEmailTemplate = """
<div style="padding: 40px; padding-top: 60px; padding-bottom: 80px; background-color: #F5F5F5; font-family: Arial, sans-serif;">
	<div style="background-color: white; padding: 15px; padding-bottom: 25px; padding-top: 5px;">
		<h2 style="text-align: center">Gorilla Groove Password Reset</h2>
		<p style="margin-bottom: 30px;">
			It looks like you're trying to reset your Gorilla Groove password. If you aren't, that's weird
		</p>
		<a href="https://gorillagroove.net/password-reset/$PASSWORD_RESET_KEY_TEMPLATE" style="padding: 14px;background-color: #1D87BA;text-decoration: none;color: white; margin-top: 15px; font-weight: bold;">
			Reset Your Password →
		</a>
	</div>
</div>
""".trimIndent()
