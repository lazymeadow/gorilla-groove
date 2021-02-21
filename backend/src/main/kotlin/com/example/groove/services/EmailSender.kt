package com.example.groove.services.email

import com.example.groove.properties.EmailProperties
import com.example.groove.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import javax.mail.internet.MimeMessage


@Service
class EmailSender(
		private val javaMailSender: JavaMailSender,
) {

	fun sendEmail(subject: String, message: String, toEmail: String) {
		logger.info("Sending email with subject '$subject' to email '$toEmail'")
		val mimeMessage: MimeMessage = javaMailSender.createMimeMessage()

		val email = MimeMessageHelper(mimeMessage, "utf-8")
		email.setText(message, true)
		email.setFrom("Gorilla Groove <no-reply@gorillagroove.net>")
		email.setTo(toEmail)
		email.setSubject(subject)

		javaMailSender.send(mimeMessage)
	}

	companion object {
		private val logger = logger()
	}
}

@Configuration
@ConditionalOnProperty(name = ["aws.email.ses"], havingValue = "false")
class GmailJavaMailConfiguration(
		private val emailProperties: EmailProperties
) {
	@Bean
	fun getJavaMailSender(): JavaMailSender {
		val mailSender = JavaMailSenderImpl()
		val props = mailSender.javaMailProperties

		mailSender.host = "smtp.gmail.com"
		mailSender.port = 587
		mailSender.username = "shoopufnetworks@gmail.com"
		mailSender.password = emailProperties.gmailPassword

		props["mail.transport.protocol"] = "smtp"
		props["mail.smtp.auth"] = "true"
		props["mail.smtp.starttls.enable"] = "true"
		props["mail.debug"] = "false"

		return mailSender
	}
}


@Configuration
@ConditionalOnProperty(name = ["aws.email.ses"], havingValue = "true")
class SesJavaMailConfiguration(
		private val emailProperties: EmailProperties
) {
	@Bean
	fun getJavaMailSender(): JavaMailSender {
		val mailSender = JavaMailSenderImpl()
		val props = mailSender.javaMailProperties

		mailSender.host = "email-smtp.us-west-2.amazonaws.com"
		mailSender.port = 587
		mailSender.username = emailProperties.sesUsername
		mailSender.password = emailProperties.sesPassword

		props["mail.transport.protocol"] = "smtp"
		props["mail.smtp.auth"] = "true"
		props["mail.smtp.starttls.enable"] = "true"
		props["mail.smtp.starttls.required"] = "true"

		return mailSender
	}
}


