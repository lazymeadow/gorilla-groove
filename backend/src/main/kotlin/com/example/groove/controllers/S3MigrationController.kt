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
import com.example.groove.services.ArtSize
import com.example.groove.services.FileStorageService
import com.example.groove.services.ImageService
import com.example.groove.services.S3StorageService
import com.example.groove.util.DateUtils.now
import com.example.groove.util.get
import com.example.groove.util.logger
import com.example.groove.util.withoutExtension
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// It's happened like 3 times that I needed to iterate over files in S3 to update or populate something, and each time
// that I do it I delete the script when I'm done and have to figure out how to do it again. Keeping this around now
// so the next time it isn't as time consuming
@RestController
@RequestMapping("api/s3-migration")
//@Profile("!prod")
class S3MigrationController(
		s3Properties: S3Properties,
		private val trackRepository: TrackRepository,
		private val imageService: ImageService,
		private val storageService: FileStorageService
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

//	@GetMapping
	fun letsGo() {
		val (oggFoundIds, oggErrorIds) = extractDataFromBucket(bucketPrefix = "music/", shared = true) { objectSize, track ->
			track.filesizeSongOgg = objectSize
		}
		val (mp3FoundIds, mp3ErrorIds) = extractDataFromBucket(bucketPrefix = "music-mp3/", shared = true) { objectSize, track ->
			track.filesizeSongMp3 = objectSize
		}
		val (artFoundIds, artErrorIds) = extractDataFromBucket(bucketPrefix = "art/") { objectSize, track ->
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
			shared: Boolean = false,
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

			logger.info("Processing ${s3Object.key} ...")
			foundS3Ids.add(trackId)

			val track = trackRepository.get(trackId) ?: run {
				logger.error("No track found with ID $trackId!")
				tracksWithErrors.add(trackId)
				return@forEach
			}

			val tracks = if (shared) {
				trackRepository.findAllByFileName(track.fileName)
			} else {
				listOf(track)
			}

			if (tracks.size > 1) {
				val otherTrackIds = tracks.map { it.id }.filter { it != trackId }
				logger.info("$trackId shares song data with: $otherTrackIds")
			}

			tracks.forEach { sharedTrack ->
				if (sharedTrack.deleted) {
					logger.error("Track with ID $trackId has been deleted!")
					tracksWithErrors.add(trackId)
				} else {
					setTrackSizeFn(s3Object.size, sharedTrack)

					trackRepository.save(sharedTrack)
				}
			}
		}

		return foundS3Ids to tracksWithErrors
	}

	@Transactional
	@GetMapping
	fun createThumbnailArt() {
		val brokenIds: List<Long> = listOf(7212, 7213, 7214, 7215, 7216, 7217, 7218, 7219, 7220, 7221, 7222, 7228, 7938)

		brokenIds.forEach { trackId ->
			val track = trackRepository.get(trackId) ?: run {
				logger.error("Could not find track with ID: $trackId")
				return@forEach
			}

			val art = storageService.loadAlbumArt(trackId, ArtSize.LARGE) ?: run {
				logger.error("No art found with track ID: $trackId")
				return@forEach
			}
			val thumbnailArt = imageService.convertToStandardArtFile(art, ArtSize.SMALL, cropToSquare = false) ?: run {
				logger.error("Could not create thumbnail art for track ID: $trackId")
				return@forEach
			}

			storageService.storeAlbumArt(thumbnailArt, trackId, ArtSize.SMALL)

			track.artUpdatedAt = now()
			track.updatedAt = track.artUpdatedAt
			track.filesizeThumbnail64x64Png = thumbnailArt.length()
			trackRepository.save(track)
		}
	}

	companion object {
		val logger = logger()
	}
}