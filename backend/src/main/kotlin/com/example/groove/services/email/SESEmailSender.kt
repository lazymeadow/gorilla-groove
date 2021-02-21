package com.example.groove.services.email

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.example.groove.properties.S3Properties
import com.example.groove.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["aws.email.ses"], havingValue = "true")
class SESEmailSender(
	s3Properties: S3Properties
) : EmailSender() {

	private val s3Client: AmazonS3

	init {
		val awsCredentials = BasicAWSCredentials(s3Properties.awsAccessKeyId, s3Properties.awsSecretAccessKey)

		s3Client = AmazonS3ClientBuilder
				.standard()
				.withRegion(Regions.US_WEST_2)
				.withCredentials(AWSStaticCredentialsProvider(awsCredentials))
				.build()
	}

	override fun sendEmail(subject: String, message: String, toEmail: String) {
		TODO("Not yet implemented")
	}

	companion object {
		private val logger = logger()

		const val sesFromEmail = "no-reply@gorillagroove.net"
	}
}
