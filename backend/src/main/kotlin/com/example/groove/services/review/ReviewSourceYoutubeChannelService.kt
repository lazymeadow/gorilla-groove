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
import com.example.groove.util.splitFirst
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

			newVideos.forEach saveLoop@ { video ->
				logger.info("Processing ${video.title}...")
				// Some channels will put out mixes every now and then. I don't really want to download mixes automatically as they could be huge, and don't really fit the GG spirit of adding individual songs
				if (video.duration > MAX_VIDEO_LENGTH) {
					logger.info("Video ${video.title} from ${video.channelTitle} has a duration of ${video.duration} which exceeds our max allowed duration of $MAX_VIDEO_LENGTH. It will be skipped")
					return@saveLoop
				}

				val (artist, name) = splitSongNameAndArtist(video.title)
				val downloadDTO = YoutubeDownloadDTO(
						url = video.videoUrl,
						name = name,
						artist = artist,
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
				logger.info("Done with ${video.title}")
			}

			source.lastSearched = searchedTime
			reviewSourceYoutubeChannelRepository.save(source)
		}

		logger.info("Review Source Youtube Channel Downloader complete")
	}

	fun splitSongNameAndArtist(songInfo: String): Pair<String, String> {
		val (finalArtist, rawTitle) = if (songInfo.contains("-")) {
			songInfo.splitFirst("-")
		} else {
			"" to songInfo
		}

		// These are bits of pointless noise in some of the channels I use this on.
		// I'd like to make this be something users can customize on the fly to suit their channels
		val thingsToStrip = setOf(
				"(lyrics)",
				"(lyric video)",
				"[Monstercat Release]",
				"[Monstercat Lyric Video]",
				"(Official Video)"
		)

		var finalTitle = rawTitle
		thingsToStrip.forEach { thingToStrip ->
			finalTitle = finalTitle.replace(thingToStrip, "", ignoreCase = true)
		}

		return finalArtist.trim() to finalTitle.trim()
	}

	fun subscribeToUser(youtubeName: String) {
		val ownUser = loadLoggedInUser()
		logger.info("Subscribing ${ownUser.name} to channel $youtubeName")

		val youtubeUserInfo = youtubeApiClient.getChannelInfoByUsername(youtubeName)
				?: throw IllegalArgumentException("Unable to find YouTube channel with name $youtubeName!")

		// Check if someone is already subscribed to this channel
		// Need to grab ID first because name is unreliable

		saveAndSubscribeToChannel(youtubeUserInfo, ownUser)
	}

	fun subscribeToChannelId(channelId: String) {
		val ownUser = loadLoggedInUser()
		logger.info("Subscribing ${ownUser.name} to channel $channelId")

		// Check if someone is already subscribed to this channel
		reviewSourceYoutubeChannelRepository.findByChannelId(channelId)?.let { reviewSource ->
			logger.info("$channelId (${reviewSource.channelName}) already exists")
			reviewSource.subscribedUsers.find { it.id == ownUser.id }?.let {
				throw IllegalArgumentException("User ${ownUser.name} is already subscribed to ${reviewSource.channelName}!")
			}
			if (reviewSource.subscribedUsers.isEmpty()) {
				logger.info("$channelId (${reviewSource.channelName}) already exists but has no users subscribed. Resetting its last searched")
				reviewSource.lastSearched = now() // This review source was inactive, so reset its lastSearched as if it was created new
			}
			reviewSource.subscribedUsers.add(ownUser)
			reviewSourceYoutubeChannelRepository.save(reviewSource)
			return
		}

		val youtubeUserInfo = youtubeApiClient.getChannelInfoByChannelId(channelId)
				?: throw IllegalArgumentException("Unable to find YouTube channel with id $channelId!")

		saveAndSubscribeToChannel(youtubeUserInfo, ownUser)
	}

	private fun saveAndSubscribeToChannel(channelInfo: YoutubeChannelInfo, user: User) {
		reviewSourceYoutubeChannelRepository.findByChannelId(channelInfo.id)?.let { reviewSource ->
			logger.info("${reviewSource.channelName} (${reviewSource.channelId}) already exists")
			reviewSource.subscribedUsers.find { it.id == user.id }?.let {
				throw IllegalArgumentException("User ${user.name} is already subscribed to ${reviewSource.channelName}! (${reviewSource.channelId})")
			}
			if (reviewSource.subscribedUsers.isEmpty()) {
				logger.info("${reviewSource.channelName} already exists but has no users subscribed. Resetting its last searched")
				reviewSource.lastSearched = now() // This review source was inactive, so reset its lastSearched as if it was created new
			}
			reviewSource.subscribedUsers.add(user)
			reviewSourceYoutubeChannelRepository.save(reviewSource)
			return
		}

		logger.info("Channel ${channelInfo.title} is new. Saving a new record")
		ReviewSourceYoutubeChannel(channelId = channelInfo.id, channelName = channelInfo.title).also {
			it.subscribedUsers.add(user)
			reviewSourceYoutubeChannelRepository.save(it)
		}
	}

	companion object {
		val logger = logger()

		const val MAX_VIDEO_LENGTH = 15 * 60 // 15 minutes
	}
}
