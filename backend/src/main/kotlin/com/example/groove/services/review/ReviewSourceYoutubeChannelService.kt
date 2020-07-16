package com.example.groove.services.review

import com.example.groove.db.dao.ReviewSourceYoutubeChannelRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.ReviewSourceYoutubeChannel
import com.example.groove.db.model.User
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.services.TrackService
import com.example.groove.services.YoutubeApiClient
import com.example.groove.services.YoutubeChannelInfo
import com.example.groove.services.YoutubeDownloadService
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewSourceYoutubeChannelService(
		private val youtubeApiClient: YoutubeApiClient,
		private val reviewSourceYoutubeChannelRepository: ReviewSourceYoutubeChannelRepository,
		private val youtubeDownloadService: YoutubeDownloadService,
		private val trackService: TrackService,
		private val trackRepository: TrackRepository
) {
	@Scheduled(cron = "0 0 8 * * *") // 8 AM every day (UTC)
	@Transactional
	fun downloadNewSongs() {
		val allSources = reviewSourceYoutubeChannelRepository.findAll()

		logger.info("Running Review Source Youtube Channel Downloader")

		allSources.forEach { source ->
			logger.info("Checking for new YouTube videos for channel: ${source.channelName} ...")
			val users = source.subscribedUsers

			if (users.isEmpty()) {
				logger.info("No users were set up for review source with ID: ${source.channelName}. Skipping")
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
				track.reviewSource = source
				track.inReview = true
				track.lastReviewed = now()
				trackRepository.save(track)


				// The YT download service will save the Track for the user that downloads it.
				// So for every other user make a copy of that track
				otherUsers.forEach { otherUser ->
					trackService.saveTrackForUserReview(otherUser, track, source)
				}
			}

			source.lastSearched = searchedTime
			reviewSourceYoutubeChannelRepository.save(source)
		}

		logger.info("Review Source Youtube Channel Downloader complete")
	}

	fun subscribeToUser(youtubeName: String) {
		val ownUser = loadLoggedInUser()

		// Check if someone is already subscribed to this channel
		reviewSourceYoutubeChannelRepository.findByChannelName(youtubeName)?.let { reviewSource ->
			reviewSource.subscribedUsers.find { it.id == ownUser.id }?.let {
				throw IllegalArgumentException("User ${ownUser.name} is already subscribed to $youtubeName!")
			}
			if (reviewSource.subscribedUsers.isEmpty()) {
				reviewSource.lastSearched = now() // This review source was inactive, so reset its lastSearched as if it was created new
			}
			reviewSource.subscribedUsers.add(ownUser)
			return
		}

		val youtubeUserInfo = youtubeApiClient.getChannelInfoByUsername(youtubeName)
				?: throw IllegalArgumentException("Unable to find YouTube channel with name $youtubeName!")

		saveAndSubscribeToChannel(youtubeUserInfo, ownUser)
	}

	fun subscribeToChannelId(channelId: String) {
		val ownUser = loadLoggedInUser()

		// Check if someone is already subscribed to this channel
		reviewSourceYoutubeChannelRepository.findByChannelId(channelId)?.let { reviewSource ->
			reviewSource.subscribedUsers.find { it.id == ownUser.id }?.let {
				throw IllegalArgumentException("User ${ownUser.name} is already subscribed to ${reviewSource.channelName}!")
			}
			if (reviewSource.subscribedUsers.isEmpty()) {
				reviewSource.lastSearched = now() // This review source was inactive, so reset its lastSearched as if it was created new
			}
			reviewSource.subscribedUsers.add(ownUser)
			return
		}

		val youtubeUserInfo = youtubeApiClient.getChannelInfoByChannelId(channelId)
				?: throw IllegalArgumentException("Unable to find YouTube channel with id $channelId!")

		saveAndSubscribeToChannel(youtubeUserInfo, ownUser)
	}

	private fun saveAndSubscribeToChannel(channelInfo: YoutubeChannelInfo, user: User) {
		ReviewSourceYoutubeChannel(channelId = channelInfo.id, channelName = channelInfo.title).also {
			it.subscribedUsers.add(user)
			reviewSourceYoutubeChannelRepository.save(it)
		}
	}

	companion object {
		val logger = logger()
	}
}
