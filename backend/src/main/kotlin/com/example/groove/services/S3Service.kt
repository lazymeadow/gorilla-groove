package com.example.groove.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.example.groove.properties.S3Properties
import org.springframework.stereotype.Service
import java.io.File

@Service
class S3Service(
		s3Properties: S3Properties
) {
	private val s3Client: AmazonS3

	private val bucketName = "gorilla-tracks"

	init {
		val awsCredentials = BasicAWSCredentials(s3Properties.awsAccessKeyId, s3Properties.awsSecretAccessKey)

		s3Client = AmazonS3ClientBuilder
				.standard()
				.withRegion(Regions.US_WEST_2)
				.withCredentials(AWSStaticCredentialsProvider(awsCredentials))
				.build()
	}

	fun uploadSongToS3(song: File) {
		s3Client.putObject(bucketName, "music/${song.name}", song)
	}

	fun uploadAlbumArtToS3(trackId: Long, albumArt: File) {
		s3Client.putObject(bucketName, "art/$trackId", albumArt)
	}

	fun testS3IGuess() {
		System.out.println("Listing buckets");
//		s3Client.listBuckets()
		s3Client.listBuckets().forEach {
			System.out.println(it.name)
		}
//		s3Client.generatePresignedUrl()
	}
}
