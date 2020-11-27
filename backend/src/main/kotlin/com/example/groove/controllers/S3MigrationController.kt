package com.example.groove.controllers

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.iterable.S3Objects
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.properties.S3Properties
import com.example.groove.services.S3StorageService
import com.example.groove.util.get
import com.example.groove.util.logger
import com.example.groove.util.withoutExtension
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// It's happened like 3 times that I needed to iterate over files in S3 to update or populate something, and each time
// that I do it I delete the script when I'm done and have to figure out how to do it again. Keeping this around now
// so the next time it isn't as time consuming
@RestController
@RequestMapping("api/s3-migration")
@Profile("!prod")
class S3MigrationController(
		s3Properties: S3Properties,
		private val trackRepository: TrackRepository
) {
	final val s3Client: AmazonS3

	init {
		val awsCredentials = BasicAWSCredentials(s3Properties.awsAccessKeyId, s3Properties.awsSecretAccessKey)

		s3Client = AmazonS3ClientBuilder
				.standard()
				.withRegion(Regions.US_WEST_2)
				.withCredentials(AWSStaticCredentialsProvider(awsCredentials))
				.build()
	}

	@GetMapping
	fun letsGo() {
		val (oggFoundIds, oggErrorIds) = extractDataFromBucket(bucketPrefix = "music/") { objectSize, track ->
			track.filesizeSongOgg = objectSize
		}
		val (mp3FoundIds, mp3ErrorIds) = extractDataFromBucket(bucketPrefix = "music-mp3/") { objectSize, track ->
			track.filesizeSongMp3 = objectSize
		}
		val (artFoundIds, artErrorIds) = extractDataFromBucket(bucketPrefix = "art/", setImageFoundOnSuccess = true) { objectSize, track ->
			track.filesizeArtPng = objectSize
		}
		val (artFoundIdsThumb, artErrorIdsThumb) = extractDataFromBucket(bucketPrefix = "art-64x64/") { objectSize, track ->
			track.filesizeThumbnail64x64Png = objectSize
		}

		val tracksWithErrors = oggErrorIds + mp3ErrorIds + artErrorIds + artErrorIdsThumb

		logger.error("Tracks with errors: $tracksWithErrors")

		logger.error("Song mismatches 1: ${oggFoundIds - mp3FoundIds}")
		logger.error("Song mismatches 2: ${mp3FoundIds - oggFoundIds}")

		logger.error("Art mismatches 1: ${artFoundIds - artFoundIdsThumb}")
		logger.error("Art mismatches 2: ${artFoundIdsThumb - artFoundIds}")

		logger.info("All done migrating")
	}

	private fun extractDataFromBucket(
			bucketPrefix: String,
			setImageFoundOnSuccess: Boolean = false,
			setTrackSizeFn: (Long, Track) -> Unit
	): Pair<Set<Long>, Set<Long>> {
		val tracksWithErrors = mutableSetOf<Long>()
		val foundS3Ids = mutableSetOf<Long>()

		S3Objects.withPrefix(s3Client, S3StorageService.bucketName, bucketPrefix).forEach { s3Object ->

			if (s3Object.key == bucketPrefix) {
				logger.info("Ignoring root folder")
				return@forEach
			}

			val trackId = s3Object.key.removePrefix(bucketPrefix).withoutExtension().toLong()
			if (trackId > 796) {
				return@forEach
			}

			logger.info("Processing ${s3Object.key} ...")
			foundS3Ids.add(trackId)

			val track = trackRepository.get(trackId) ?: run {
				logger.error("No track found with ID $trackId!")
				tracksWithErrors.add(trackId)
				return@forEach
			}

			if (track.deleted) {
				logger.error("Track with ID $trackId has been deleted!")
				tracksWithErrors.add(trackId)
				return@forEach
			}

			setTrackSizeFn(s3Object.size, track)

			if (setImageFoundOnSuccess) {
				track.hasArt = true
			}

			trackRepository.save(track)
		}

		return foundS3Ids to tracksWithErrors
	}

	companion object {
		val logger = logger()
	}
}