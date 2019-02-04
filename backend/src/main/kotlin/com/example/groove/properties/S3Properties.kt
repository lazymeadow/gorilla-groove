package com.example.groove.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class S3Properties {
	@Value("\${aws.access.key.id}")
	val awsAccessKeyId: String? = null

	@Value("\${aws.secret.access.key}")
	val awsSecretAccessKey: String? = null

	@Value("#{new Boolean('\${aws.store.in.s3}')}")
	val awsStoreInS3: Boolean = false
}
