package com.example.groove.services.email

@Suppress("SameParameterValue")
abstract class EmailSender {
	abstract fun sendEmail(
			subject: String,
			message: String,
			toEmail: String
	)
}
