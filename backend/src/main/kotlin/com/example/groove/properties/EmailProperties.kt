package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EmailProperties {
	@Value("\${smtp.mail.password:#{null}}")
	val smtpPassword: String? = null
		get() = when (field) {
			null -> throw IllegalStateException("Missing smtpPassword key in configuration! Configure with 'smtp.mail.password'")
			"EXAMPLE_PASSWORD" -> throw IllegalStateException("smtpPassword key is using the default 'EXAMPLE_PASSWORD'! Configure 'smtp.mail.password'")
			else -> field
		}
}