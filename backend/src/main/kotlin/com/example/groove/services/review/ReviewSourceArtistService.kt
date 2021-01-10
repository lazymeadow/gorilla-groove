package com.example.groove.services.review

import com.example.groove.db.dao.ReviewSourceArtistDownloadRepository
import com.example.groove.db.dao.ReviewSourceArtistRepository
import com.example.groove.db.dao.ReviewSourceUserRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.ReviewSourceArtist
import com.example.groove.db.model.ReviewSourceArtistDownload
import com.example.groove.db.model.ReviewSourceUser
import com.example.groove.db.model.User
import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.properties.S3Properties
import com.example.groove.services.*
import com.example.groove.services.socket.ReviewQueueSocketHandler
import com.example.groove.util.DateUtils.now
import com.example.groove.util.firstAndRest
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.example.groove.util.minusWeeks
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewSourceArtistService(
		private val spotifyApiClient: SpotifyApiClient,
		private val reviewSourceArtistRepository: ReviewSourceArtistRepository,
		private val reviewSourceArtistDownloadRepository: ReviewSourceArtistDownloadRepository,
		private val reviewSourceUserRepository: ReviewSourceUserRepository,
		private val youtubeDownloadService: YoutubeDownloadService,
		private val trackService: TrackService,
		private val trackRepository: TrackRepository,
		private val reviewQueueSocketHandler: ReviewQueueSocketHandler,
		private val environment: Environment,
		private val s3Properties: S3Properties
) {
	@Scheduled(cron = "0 0 9 * * *") // 9 AM every day (UTC)
	@Transactional
	fun downloadNewSongs() {
		// Dev computers sometimes need to turn on 'awsStoreInS3' to run scripts against prod data, and if these
		// jobs run while that is active, they will overwrite data in S3. This is an added protection against that.
		if (s3Properties.awsStoreInS3 && !environment.activeProfiles.contains("prod")) {
			logger.info("S3 storage is on without the prod profile active. Not running Artist job")
			return
		}

		val allSources = reviewSourceArtistRepository.findAll()

		logger.info("Running Review Source Artist Downloader")

		allSources.forEach { processSource(it) }

		logger.info("Review Source Artist Downloader complete")
	}

	// This process can take a really long time, and we need a transaction to avoid errors like
	// "could not initialize proxy - no Session". However, I don't want one transaction because
	// then if we have an issue everything gets rolled back. Function is public only to allow
	// @Transactional annotation to work because it is a limitation of Spring Boot
	@Transactional
	fun processSource(source: ReviewSourceArtist) {
		logger.info("Checking for new songs for artist: ${source.artistName} ...")
		if (!source.isActive()) {
			logger.info("Review source for artist ${source.artistName} is not active. Skipping")
			return
		}

		val users = source.getActiveUsers()

		// Spotify song releases have "day" granularity, so don't include a time component.
		// Also, Spotify might not always get songs on their service right when they are new, so
		// we can't really keep a rolling search. The way the API works right now we have to request
		// everything every time by the artist anyway.... so just grab everything from the artist, unless
		// the user specified only newer things than a certain date when creating the review source.
		val searchNewerThan = source.searchNewerThan?.toLocalDateTime()?.toLocalDate()

		val newSongs = spotifyApiClient.getSongsByArtist(source.artistName, source.artistId, searchNewerThan)
		logger.info("Found ${newSongs.size} initial songs for artist: ${source.artistName}")

		// We are going to be searching over the same music every night. Nothing we can do about it really.
		// Just have to play around the way the Spotify API works. So we have to keep track manually on what
		// songs we've already seen by a given artist
		val existingSongDiscoveries = reviewSourceArtistDownloadRepository.findByReviewSource(source)
		val discoveredSongNameToDownloadAttempt = existingSongDiscoveries.map {
			it.trackName.toLowerCase() to it
		}.toMap()

		val oneWeekAgo = now().minusWeeks(1)
		val songsToDownload = newSongs.filter { newSong ->
			discoveredSongNameToDownloadAttempt[newSong.name.toLowerCase()]?.let { artistDownload ->
				// If the download was successful a prior time, exclude it from the songs to download
				if (artistDownload.downloadedAt != null) {
					return@filter false
				}

				// Otherwise, we have attempted this download before and it failed. We should only retry
				// once a week, as we have a limited quota and there's a good chance this was not a transient error
				return@filter artistDownload.lastDownloadAttempt?.before(oneWeekAgo) ?: true
			}

			// Otherwise, this is a song we've never seen before. Add a record of it
			ReviewSourceArtistDownload(
					reviewSource = source,
					trackName = newSong.name
			).also { reviewSourceArtistDownloadRepository.save(it) }

			return@filter true
		}
		logger.info("Attempting download on ${songsToDownload.size} ${source.artistName} songs")

		val downloadCount = attemptDownloadFromYoutube(source, songsToDownload, users)
		if (downloadCount > 0) {
			users.forEach { user ->
				reviewQueueSocketHandler.broadcastNewReviewQueueContent(user.id, source, downloadCount)
			}
		}
	}

	// Now we need to find a match on YouTube to download...
	private fun attemptDownloadFromYoutube(source: ReviewSourceArtist, songsToDownload: List<MetadataResponseDTO>, users: List<User>): Int {
		val (firstUser, otherUsers) = users.firstAndRest()

		var downloadCount = 0

		songsToDownload.forEach { song ->
			val artistDownload = reviewSourceArtistDownloadRepository.findByReviewSourceAndTrackName(source, song.name)
					?: throw IllegalStateException("Could not locate an artist download with review source ID: ${source.id} and song name ${song.name}!")
			val videos = youtubeDownloadService.searchYouTube("${song.artist} ${song.name}", targetLength = song.length)

			// There's a chance that our search yields no valid results, but youtube will pretty much always return
			// us videos, even if they're a horrible match. Probably the best thing we can do is check the video title
			// and duration to sanity check and make sure it is at least a decent match.
			// Better to NOT find something than to find a video which isn't correct for something like this...
			val validVideos = videos.filter { it.isValidForSong(song) }

			if (validVideos.isEmpty()) {
				logger.warn("Could not find a valid YouTube download for ${song.artist} - ${song.name}")

				artistDownload.lastDownloadAttempt = now()
				reviewSourceArtistDownloadRepository.save(artistDownload)

				return@forEach
			}

			// For now, just rely on YouTube's relevance to give us the best result (so take the first one).
			// Might be better to try to exclude music videos and stuff later, though the time checking might help already.
			val video = validVideos.first()

			logger.info("Found a valid match. YouTube video title: ${video.title}")
			val downloadDTO = YoutubeDownloadDTO(
					url = video.videoUrl,
					name = song.name,
					artist = song.artist,
					album = song.album,
					releaseYear = song.releaseYear,

					// Because we started from Spotify, we have a URL to the actual album art.
					// This is better than whatever it is we will get from the YT download, so pass it along to be used instead
					artUrl = song.albumArtLink,
					cropArtToSquare = true
			)

			// Sometimes YoutubeDL can have issues. Don't cascade fail all downloads because of it
			val track = try {
				youtubeDownloadService.downloadSong(firstUser, downloadDTO)
			} catch (e: Exception) {
				logger.error("Failed to download from YouTube for ${song.artist} - ${song.name}!", e)

				artistDownload.lastDownloadAttempt = now()
				reviewSourceArtistDownloadRepository.save(artistDownload)

				return@forEach
			}

			track.reviewSource = source
			track.inReview = true
			track.lastReviewed = now()
			trackRepository.save(track)

			artistDownload.lastDownloadAttempt = now()
			artistDownload.downloadedAt = artistDownload.lastDownloadAttempt
			reviewSourceArtistDownloadRepository.save(artistDownload)

			// The YT download service will save the Track for the user that downloads it.
			// So for every other user make a copy of that track
			otherUsers.forEach { otherUser ->
				trackService.saveTrackForUserReview(otherUser, track, source)
			}

			downloadCount++
		}

		return downloadCount
	}


	private fun YoutubeDownloadService.VideoProperties.isValidForSong(song: MetadataResponseDTO): Boolean {
		val lowerTitle = this.title.toLowerCase()
		val artist = song.artist.toLowerCase()

		// Make sure the artist is in the title somewhere OR the channel name. If it isn't that seems like a bad sign
		if (!lowerTitle.contains(artist) && !this.channelName.toLowerCase().contains(artist)) {
			return false
		}

		// Now lastly we want to check that the song title is adequately represented in the video title. I ran into
		// a lot of situations where titles were slightly different so a substring match wasn't viable. So I think
		// a better approach is to check each word individually for representation, and get rid of words that have
		// little value or little hope or being matched correctly
		val unimportantWords = setOf("with", "feat", "ft", "featuring")
		val titleWords = this.title
				.toLowerCase()
				.replace("(", "")
				.replace(")", "")
				.replace(".", "")
				.replace("-", "")
				.split(" ")
				.filter { it.isNotBlank() && !unimportantWords.contains(it) }

		return titleWords.all { lowerTitle.contains(it) }
	}

	fun subscribeToArtist(artistName: String): Pair<ReviewSourceUser?, List<String>> {
		val currentUser = loadLoggedInUser()

		// First, check if the artist already exists. Someone may have subscribed to them earlier
		reviewSourceArtistRepository.findByArtistName(artistName)?.let { existing ->
			if (existing.isUserSubscribed(currentUser)) {
				throw IllegalArgumentException("User is already subscribed to artist $artistName!")
			}

			// If this source was previously unused, re-enable it and update the time to search from to now
			if (existing.getActiveUsers().isEmpty()) {
				logger.info("Source for $artistName already exists but has no subscribed users. Resetting its 'searchNewerThan'")
				existing.updatedAt = now()
				existing.searchNewerThan = now()

				reviewSourceArtistRepository.save(existing)
			}

			// If the source already existed, then the association might as well
			reviewSourceUserRepository.findByUserAndSource(userId = currentUser.id, sourceId = existing.id)?.let { sourceAssociation ->
				// Association was previously deleted. Just re-enable it and return early
				sourceAssociation.deleted = false
				sourceAssociation.updatedAt = now()
				reviewSourceUserRepository.save(sourceAssociation)

				return sourceAssociation to emptyList()
			}

			val reviewSourceUser = ReviewSourceUser(reviewSource = existing, user = currentUser)
			reviewSourceUserRepository.save(reviewSourceUser)

			return reviewSourceUser to emptyList()
		}

		// Ok so nobody has subscribed to this artist before. We need to create a new one and add our user to it

		// Start by finding the artists Spotify gives us based off the name
		val artists = spotifyApiClient.searchArtistsByName(artistName)

		// Try to find an exact match. If we don't, return a list of artists to try to help the end user
		// fix any typos or things of that nature
		val targetName = artistName.toLowerCase()
		val foundArtist = artists.find { it.name.toLowerCase() == targetName }
				?: return null to artists.map { it.name }.take(5)

		// Cool cool cool we found an artist in spotify. Now just save it.
		// Hard-code "searchNewerThan" to now() until we are not throttled by YouTube and can search unlimited
		val source = ReviewSourceArtist(artistId = foundArtist.id, artistName = foundArtist.name, searchNewerThan = now())
		reviewSourceArtistRepository.save(source)

		val reviewSourceUser = ReviewSourceUser(reviewSource = source, user = currentUser)
		reviewSourceUserRepository.save(reviewSourceUser)

		return reviewSourceUser to emptyList()
	}

	companion object {
		private val logger = logger()
	}
}
