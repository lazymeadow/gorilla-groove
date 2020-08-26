package com.example.groove.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.iterable.S3Objects
import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.S3Properties
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.FileUtils
import com.example.groove.util.logger
import com.example.groove.util.withNewExtension
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Service
@ConditionalOnProperty(name = ["aws.store.in.s3"], havingValue = "false")
class S3StorageService(
		s3Properties: S3Properties,
		trackRepository: TrackRepository,
		trackLinkRepository: TrackLinkRepository,
		fileStorageProperties: FileStorageProperties,
		private val imageService: ImageService,
		private val fileUtils: FileUtils
) : FileStorageService(trackRepository, trackLinkRepository, fileStorageProperties, fileUtils) {

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

	fun temporaryMigrationOfAlbumArt() {
		var backToNormal = false
		S3Objects.withPrefix(s3Client, bucketName, "art/").forEach { objectSummary ->
			// This happens once, because the directory containing the objects is returned from the query...
			if (objectSummary.key == "art/") {
				return@forEach
			}
			val trackId = objectSummary.key.split("/").last().split(".").first().toInt()
			if (trackId == 7211) {
				println("Back to normal")
				backToNormal = true
			}

			if (!backToNormal) {
				return@forEach
			}

			val largeArtStream = s3Client.getObject(bucketName, objectSummary.key).objectContent

			println("Processing track ID $trackId")

			val largeArt = fileUtils.createTemporaryFile(".png")
			Files.copy(largeArtStream, largeArt.toPath(), StandardCopyOption.REPLACE_EXISTING)

			val convertedLargeArt = imageService.convertToStandardArtFile(largeArt, ArtSize.LARGE, false) ?: return@forEach
			val convertedSmallArt = imageService.convertToStandardArtFile(largeArt, ArtSize.SMALL, false) ?: return@forEach

			storeAlbumArt(convertedLargeArt, trackId.toLong(), ArtSize.LARGE)
			storeAlbumArt(convertedSmallArt, trackId.toLong(), ArtSize.SMALL)
		}
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

	override fun storeAlbumArt(albumArt: File, trackId: Long, artSize: ArtSize) {
		s3Client.putObject(bucketName, "${artSize.s3Directory}/$trackId.png", albumArt)
	}

	override fun loadAlbumArt(trackId: Long, artSize: ArtSize): File? {
		val path = "${artSize.s3Directory}/$trackId.png"

		val exists = s3Client.doesObjectExist(bucketName, path)
		if (!exists) {
			return null
		}

		val s3Stream = s3Client.getObject(bucketName, path).objectContent
		val filePath = generateTmpFilePath()
		Files.copy(s3Stream, filePath, StandardCopyOption.REPLACE_EXISTING)

		return filePath.toFile()
	}

	override fun copyAllAlbumArt(trackSourceId: Long, trackDestinationId: Long) {
		ArtSize.values().forEach { artSize ->
			if (s3Client.doesObjectExist(bucketName, "${artSize.s3Directory}/$trackSourceId.png")) {
				s3Client.copyObject(
						bucketName,
						"${artSize.s3Directory}/$trackSourceId.png",
						bucketName,
						"${artSize.s3Directory}/$trackDestinationId.png"
				)
			}
		}
	}

	override fun deleteSong(fileName: String) {
		AudioFormat.values().forEach { format ->
			val key = "${format.s3Directory}/${fileName.withNewExtension(format.extension)}"
			s3Client.deleteObject(bucketName, key)
		}
	}

	override fun getSongLink(trackId: Long, anonymousAccess: Boolean, audioFormat: AudioFormat): String {
		return getCachedTrackLink(trackId, anonymousAccess, audioFormat, false) { track ->
			val key = "${audioFormat.s3Directory}/${track.fileName.withNewExtension(audioFormat.extension)}"
			s3Client.generatePresignedUrl(bucketName, key, expireHoursOut(4)).toString()
		}
	}

	override fun getAlbumArtLink(trackId: Long, anonymousAccess: Boolean, artSize: ArtSize): String? {
		return getCachedTrackLink(trackId, anonymousAccess, isArtLink = true, artSize = artSize) { track ->
			s3Client.generatePresignedUrl(bucketName, "${artSize.s3Directory}/${track.id}.png", expireHoursOut(4)).toString()
		}
	}

	override fun copySong(sourceFileName: String, destinationFileName: String, audioFormat: AudioFormat) {
		val sourceKey = "${audioFormat.s3Directory}/$sourceFileName"
		val destKey = "${audioFormat.s3Directory}/$destinationFileName"

		s3Client.copyObject(bucketName, sourceKey, bucketName, destKey)
	}

	companion object {
		val logger = logger()
	}
}
