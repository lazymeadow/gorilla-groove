package com.example.groove.services

import com.example.groove.db.dao.ReviewSourceRepository
import com.example.groove.db.dao.ReviewSourceYoutubeChannelRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.TrackReviewInfoRepository
import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.Track
import com.example.groove.db.model.TrackReviewInfo
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.util.DateUtils.now
import com.example.groove.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewSourceYoutubeChannelService(
		private val youtubeApiClient: YoutubeApiClient,
		private val reviewSourceYoutubeChannelRepository: ReviewSourceYoutubeChannelRepository,
		private val reviewSourceRepository: ReviewSourceRepository,
		private val youtubeDownloadService: YoutubeDownloadService,
		private val trackRepository: TrackRepository,
		private val trackReviewInfoRepository: TrackReviewInfoRepository
) {
	@Scheduled(cron = "0 0 8 * * *") // 8 AM every day (UTC)
	@Transactional
	fun downloadNewSongs() {
		val allSources = reviewSourceYoutubeChannelRepository.findAll()

		logger.info("Running Review Source Youtube Channel Downloader")

		allSources.forEach { source ->
			logger.info("Checking for new YouTube videos for channel: ${source.channelName} ...")
			val reviewSource = reviewSourceRepository.findByReviewSourceImplementationId(source.id)
					?: throw IllegalStateException("No review source found for Youtube source ID: ${source.id}!")

			val users = reviewSource.subscribedUsers

			if (users.isEmpty()) {
				logger.error("No users were set up for review source with ID: ${reviewSource.id}! It should be deleted!")
				return@forEach
			}

			// We establish our own upper limit. In the very unlikely, though possible, event that a video
			// is uploaded between us creating this timestamp, and the request getting to YouTube, we need
			// to filter out the new videos or we will save it twice (since we would search for it
			// again when we run this job the next time)
			val searchedTime = now()
			val newVideos = youtubeApiClient
					.findVideos(channelId = source.channelId)
					.videos
					.filter { it.publishedAt > source.lastSearched && it.publishedAt < searchedTime}
			logger.info("Found ${newVideos.size} new video(s) for channel: ${source.channelName}")

			val (firstUser, otherUsers) = users.partition { it.id == users.first().id }

			newVideos.forEach { video ->
				val downloadDTO = YoutubeDownloadDTO(
						url = video.videoUrl,
						name = video.title,
						cropArtToSquare = true
				)
				val track = youtubeDownloadService.downloadSong(firstUser.first(), downloadDTO)
				track.saveReviewInfo(reviewSource)

				// The YT download service will save the Track for the user that downloads it.
				// So for every other user just copy that DB entity and give it the user. It will
				// point to the same S3 bucket for everything, however, we don't share album art
				// right now so that will need to be copied!
				// TODO album art copying
				otherUsers.forEach { otherUser ->
					val otherUserTrack = track.copy(id = 0, user = otherUser)
					trackRepository.save(otherUserTrack)
					otherUserTrack.saveReviewInfo(reviewSource)
				}
			}

			source.lastSearched = searchedTime
			reviewSourceYoutubeChannelRepository.save(source)
		}

		logger.info("Review Source Youtube Channel Downloader complete")
	}

	private fun Track.saveReviewInfo(reviewSource: ReviewSource) {
		TrackReviewInfo(
				reviewSource = reviewSource,
				track = this
		).also { trackReviewInfoRepository.save(it) }
	}

	companion object {
		val logger = logger()
	}
}
