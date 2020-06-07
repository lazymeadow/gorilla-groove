package com.example.groove.services

import com.example.groove.db.dao.ReviewSourceArtistRepository
import com.example.groove.db.dao.ReviewSourceYoutubeChannelRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.util.DateUtils.now
import com.example.groove.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ReviewSourceArtistService(
		private val youtubeApiClient: YoutubeApiClient,
		private val spotifyApiClient: SpotifyApiClient,
		private val reviewSourceArtistRepository: ReviewSourceArtistRepository,
		private val youtubeDownloadService: YoutubeDownloadService,
		private val trackService: TrackService,
		private val trackRepository: TrackRepository
) {
//	@Scheduled(cron = "0 0 9 * * *") // 9 AM every day (UTC)
//	@Scheduled(cron = "0 * * * * *") // 9 AM every day (UTC)
	@Transactional
	fun downloadNewSongs() {
		val allSources = reviewSourceArtistRepository.findAll()

		logger.info("Running Review Source Artist Downloader")

		allSources.forEach { source ->
			logger.info("Checking for new songs for artist: ${source.artistName} ...")
			val users = source.subscribedUsers

			if (users.isEmpty()) {
				logger.error("No users were set up for review source with ID: ${source.id}! It should be deleted!")
				return@forEach
			}

			// Spotify song releases have "day" granularity, and we want to only get songs that are new.
			val searchDate = LocalDate.now().minusMonths(6)
			val newSongs = spotifyApiClient.getSongsByArtist(source.artistName, source.artistId, searchDate)
			logger.info("Found ${newSongs.size} new songs for artist: ${source.artistName}")

			// Now we need to find a match on YouTube to download...
			newSongs.forEach { newSong ->
				val videos = youtubeApiClient.findVideos("${newSong.artist} ${newSong.name}")
			}

//			val (firstUser, otherUsers) = users.partition { it.id == users.first().id }
//
//			newVideos.forEach { video ->
//				val downloadDTO = YoutubeDownloadDTO(
//						url = video.videoUrl,
//						name = video.title,
//						cropArtToSquare = true
//				)
//				val track = youtubeDownloadService.downloadSong(firstUser.first(), downloadDTO)
//				track.reviewSource = source
//				track.lastReviewed = now()
//				trackRepository.save(track)
//
//				// The YT download service will save the Track for the user that downloads it.
//				// So for every other user just copy that DB entity and give it the user. It will
//				// point to the same S3 bucket for everything, however, we don't share album art
//				// right now so that will need to be copied!
//				// TODO album art copying
//				otherUsers.forEach { otherUser ->
//					trackService.saveTrackForUserReview(otherUser, track, source)
//				}
//			}
//
//			source.lastSearched = searchedTime
//			reviewSourceYoutubeChannelRepository.save(source)
		}

		logger.info("Review Source Artist Downloader complete")
	}

	companion object {
		val logger = logger()
	}
}
