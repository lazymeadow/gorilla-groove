package com.example.groove.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.properties.S3Properties
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
@ConditionalOnProperty(name = ["aws.store.in.s3"], havingValue = "true")
class S3StorageService(
		s3Properties: S3Properties,
		private val trackRepository: TrackRepository
) : FileStorageService {

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

	override fun storeSong(song: File, trackId: Long) {
		s3Client.putObject(bucketName, "music/${song.name}", song)
	}

	override fun storeAlbumArt(albumArt: File, trackId: Long) {
		s3Client.putObject(bucketName, "art/$trackId", albumArt)
	}

	override fun getSongLink(trackId: Long): String {
		val track = loadAuthenticatedTrack(trackId)

		return s3Client.generatePresignedUrl(bucketName, "music/${track.fileName}", getS3ExpirationDate()).path
	}

	override fun getAlbumArtLink(trackId: Long): String? {
		val track = loadAuthenticatedTrack(trackId)

		return s3Client.generatePresignedUrl(bucketName, "art/${track.id}", getS3ExpirationDate()).path
	}

	private fun loadAuthenticatedTrack(trackId: Long): Track {
		val track = trackRepository.findById(trackId).unwrap() ?: throw IllegalArgumentException("No track with ID $trackId found")
		val user = loadLoggedInUser()

		if (track.hidden && user.id != track.user.id) {
			throw IllegalArgumentException("Insufficient privileges to access trackId $trackId")
		}

		return track
	}

	private fun getS3ExpirationDate(): Date {
		val calendar = Calendar.getInstance()
		calendar.add(Calendar.HOUR, 4)

		return calendar.time
	}

}