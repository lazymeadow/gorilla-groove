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
@ConditionalOnProperty(name = ["aws.email.ses"], havingValue = "false")
class SMTPEmailSender(
		private val javaMailSender: JavaMailSender,
) : EmailSender() {

	override fun sendEmail(subject: String, message: String, toEmail: String) {
		logger.info("Sending email with subject '$subject' to email '$toEmail'")
		val mimeMessage: MimeMessage = javaMailSender.createMimeMessage()

		val email = MimeMessageHelper(mimeMessage, "utf-8")
		email.setText(message, true)
		email.setFrom("noreply@shoopufnetworks.com")
		email.setTo(toEmail)
		email.setSubject(subject)

		javaMailSender.send(mimeMessage)
	}

	companion object {
		private val logger = logger()
	}
}

@Configuration
class JavaMailConfiguration(
		private val emailProperties: EmailProperties
) {
	@Bean
	fun getJavaMailSender(): JavaMailSender {
		val mailSender = JavaMailSenderImpl()
		mailSender.host = "smtp.gmail.com"
		mailSender.port = 587
		mailSender.username = "shoopufnetworks@gmail.com"
		mailSender.password = emailProperties.smtpPassword

		val props = mailSender.javaMailProperties
		props["mail.transport.protocol"] = "smtp"
		props["mail.smtp.auth"] = "true"
		props["mail.smtp.starttls.enable"] = "true"
		props["mail.debug"] = "true"

		return mailSender
	}
}
