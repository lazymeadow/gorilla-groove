package com.example.groove.services

import com.example.groove.services.email.EmailSender
import org.springframework.stereotype.Service
import java.util.*

private const val PASSWORD_RESET_KEY_TEMPLATE = "---key---"

@Service
class PasswordResetService(
		private val emailSender: EmailSender
) {
	fun sendPasswordResetEmail() {
		val randomKey = UUID.randomUUID().toString()
		val userEmail = "ayrtonstout@outlook.com"

		emailSender.sendEmail(
				subject = "Password Reset",
				message = passwordResetEmailTemplate.replace(PASSWORD_RESET_KEY_TEMPLATE, randomKey),
				toEmail = userEmail
		)
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
