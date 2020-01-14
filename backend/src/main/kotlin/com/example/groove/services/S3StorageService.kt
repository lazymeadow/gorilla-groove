package com.example.groove.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.S3Properties
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.withNewExtension
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Service
@ConditionalOnProperty(name = ["aws.store.in.s3"], havingValue = "true")
class S3StorageService(
		s3Properties: S3Properties,
		trackRepository: TrackRepository,
		trackLinkRepository: TrackLinkRepository,
		fileStorageProperties: FileStorageProperties
) : FileStorageService(trackRepository, trackLinkRepository, fileStorageProperties) {

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

	override fun storeSong(song: File, trackId: Long, audioFormat: AudioFormat) {
		val key = when (audioFormat) {
			AudioFormat.MP3 -> "music-mp3/$trackId${audioFormat.extension}"
			AudioFormat.OGG -> "music/$trackId${audioFormat.extension}"
		}

		logger.info("Storing song in S3 as $key")
		s3Client.putObject(bucketName, key, song)
	}

	override fun loadSong(track: Track, audioFormat: AudioFormat): File {
		val key = when (audioFormat) {
			AudioFormat.MP3 -> "music-mp3/${track.fileName.withNewExtension(audioFormat.extension)}"
			AudioFormat.OGG -> "music/${track.fileName}"
		}

		val s3Stream = s3Client.getObject(bucketName, key).objectContent
		val filePath = generateTmpFilePath()
		Files.copy(s3Stream, filePath, StandardCopyOption.REPLACE_EXISTING)

		return filePath.toFile()
	}

	override fun storeAlbumArt(albumArt: File, trackId: Long) {
		s3Client.putObject(bucketName, "art/$trackId.png", albumArt)
	}

	override fun loadAlbumArt(trackId: Long): File? {
		val path = "art/$trackId.png"

		val exists = s3Client.doesObjectExist(bucketName, path)
		if (!exists) {
			return null
		}

		val s3Stream = s3Client.getObject(bucketName, path).objectContent
		val filePath = generateTmpFilePath()
		Files.copy(s3Stream, filePath, StandardCopyOption.REPLACE_EXISTING)

		return filePath.toFile()
	}

	override fun copyAlbumArt(trackSourceId: Long, trackDestinationId: Long) {
		if (s3Client.doesObjectExist(bucketName, "art/$trackSourceId.png")) {
			s3Client.copyObject(bucketName, "art/$trackSourceId.png", bucketName, "art/$trackDestinationId.png")
		}
	}

	override fun deleteSong(fileName: String) {
		s3Client.deleteObject(bucketName, "music/$fileName")
	}

	override fun getSongLink(trackId: Long, anonymousAccess: Boolean, audioFormat: AudioFormat): String {
		return getCachedSongLink(trackId, anonymousAccess, audioFormat) { track ->
			s3Client.generatePresignedUrl(bucketName, "music/${track.fileName}", expireHoursOut(4)).toString()
		}
	}

	override fun getAlbumArtLink(trackId: Long, anonymousAccess: Boolean): String? {
		val track = getTrackForAlbumArt(trackId, anonymousAccess)

		return s3Client.generatePresignedUrl(bucketName, "art/${track.id}.png", expireHoursOut(4)).toString()
	}

	override fun copySong(sourceFileName: String, destinationFileName: String, audioFormat: AudioFormat) {
		val sourceKey = "${audioFormat.s3Bucket}/$sourceFileName"
		val destKey = "${audioFormat.s3Bucket}/$destinationFileName"

		s3Client.copyObject(bucketName, sourceKey, bucketName, destKey)
	}

	companion object {
		val logger = LoggerFactory.getLogger(S3StorageService::class.java)!!
	}
}
