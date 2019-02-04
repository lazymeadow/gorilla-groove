package com.example.groove.services

import com.example.groove.properties.S3Properties
import software.amazon.awssdk.services.s3.S3Client
import java.util.*

class S3Service(
		s3Properties: S3Properties
) {
	private val s3Client: S3Client

	//	val thing = DynamoDbClient.builder()
	init {
		val systemProperties = Properties(System.getProperties())
		systemProperties.setProperty("aws.accessKeyId", s3Properties.awsAccessKeyId)
		systemProperties.setProperty("aws.secretAccessKey", s3Properties.awsSecretAccessKey)

		System.setProperties(systemProperties)

		s3Client = S3Client.create()
	}

	fun testS3IGuess() {
		System.out.println("Listing buckets");
		s3Client.listBuckets().buckets().forEach {
				System.out.println(it.name())
		}
	}
}