package com.example.groove.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.properties.S3Properties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnProperty(name = ["aws.store.in.s3"], havingValue = "true")
class S3StorageService(
		s3Properties: S3Properties,
		trackRepository: TrackRepository,
		trackLinkRepository: TrackLinkRepository
) : FileStorageService(trackRepository, trackLinkRepository) {

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
		s3Client.putObject(bucketName, "art/$trackId.png", albumArt)
	}

	override fun copyAlbumArt(trackSourceId: Long, trackDestinationId: Long) {
		if (s3Client.doesObjectExist(bucketName, "art/$trackSourceId.png")) {
			s3Client.copyObject(bucketName, "art/$trackSourceId.png", bucketName, "art/$trackDestinationId.png")
		}
	}

	override fun deleteSong(fileName: String) {
		s3Client.deleteObject(bucketName, "music/$fileName")
	}

	override fun getSongLink(trackId: Long, anonymousAccess: Boolean): String {
		return getCachedSongLink(trackId, anonymousAccess) { track ->
			s3Client.generatePresignedUrl(bucketName, "music/${track.fileName}", expireHoursOut(4)).toString()
		}
	}

	override fun getAlbumArtLink(trackId: Long): String? {
		val track = loadAuthenticatedTrack(trackId, false)

		return s3Client.generatePresignedUrl(bucketName, "art/${track.id}.png", expireHoursOut(4)).toString()
	}

}