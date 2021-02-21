package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EmailProperties {
	@Value("\${gmail.mail.password:#{null}}")
	val gmailPassword: String? = null
		get() = when (field) {
			null -> throw IllegalStateException("Missing gmailPassword key in configuration! Configure with 'gmail.mail.password'")
			"EXAMPLE_PASSWORD" -> throw IllegalStateException("gmailPassword key is using the default 'EXAMPLE_PASSWORD'! Configure 'gmail.mail.password'")
			else -> field
		}

	@Value("\${aws.ses.user:#{null}}")
	val sesUsername: String? = null
		get() = when (field) {
			null -> throw IllegalStateException("Missing sesUsername key in configuration! Configure with 'aws.ses.user'")
			"EXAMPLE_KEY" -> throw IllegalStateException("sesUsername key is using the default 'EXAMPLE_KEY'! Configure 'aws.ses.user'")
			else -> field
		}

	@Value("\${aws.ses.password:#{null}}")
	val sesPassword: String? = null
		get() = when (field) {
			null -> throw IllegalStateException("Missing sesPassword key in configuration! Configure with 'aws.ses.password'")
			"EXAMPLE_PASSWORD" -> throw IllegalStateException("sesPassword key is using the default 'EXAMPLE_PASSWORD'! Configure 'aws.ses.password'")
			else -> field
		}

	@Value("#{new Boolean('\${aws.email.ses}')}")
	val sendEmailSes: Boolean = false
}
