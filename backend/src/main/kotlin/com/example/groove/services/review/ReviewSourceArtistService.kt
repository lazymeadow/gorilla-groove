package com.example.groove.services.review

import com.example.groove.db.dao.ReviewSourceArtistDownloadRepository
import com.example.groove.db.dao.ReviewSourceArtistRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.ReviewSourceArtist
import com.example.groove.db.model.ReviewSourceArtistDownload
import com.example.groove.db.model.User
import com.example.groove.dto.MetadataResponseDTO
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.services.*
import com.example.groove.services.socket.ReviewQueueSocketHandler
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.example.groove.util.minusWeeks
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewSourceArtistService(
		private val youtubeApiClient: YoutubeApiClient,
		private val spotifyApiClient: SpotifyApiClient,
		private val reviewSourceArtistRepository: ReviewSourceArtistRepository,
		private val reviewSourceArtistDownloadRepository: ReviewSourceArtistDownloadRepository,
		private val youtubeDownloadService: YoutubeDownloadService,
		private val trackService: TrackService,
		private val trackRepository: TrackRepository,
		private val imageService: ImageService,
		private val songIngestionService: SongIngestionService,
		private val reviewQueueSocketHandler: ReviewQueueSocketHandler
) {
	@Scheduled(cron = "0 0 9 * * *") // 9 AM every day (UTC)
	@Transactional
	fun downloadNewSongs() {
		val allSources = reviewSourceArtistRepository.findAll()

		logger.info("Running Review Source Artist Downloader")

		allSources.forEach { source ->
			logger.info("Checking for new songs for artist: ${source.artistName} ...")
			val users = source.subscribedUsers

			if (users.isEmpty()) {
				logger.info("No users were set up for review source ${source.artistName}. Skipping")
				return@forEach
			}

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
				it.trackName to it
			}.toMap()

			val oneWeekAgo = now().minusWeeks(1)
			val songsToDownload = newSongs.filter { newSong ->
				discoveredSongNameToDownloadAttempt[newSong.name]?.let { artistDownload ->
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

		logger.info("Review Source Artist Downloader complete")
	}

	// Now we need to find a match on YouTube to download...
	private fun attemptDownloadFromYoutube(source: ReviewSourceArtist, songsToDownload: List<MetadataResponseDTO>, users: List<User>): Int {
		val (firstUser, otherUsers) = users.partition { it.id == users.first().id }

		var downloadCount = 0

		songsToDownload.forEach { song ->
			val artistDownload = reviewSourceArtistDownloadRepository.findByReviewSourceAndTrackName(source, song.name)
					?: throw IllegalStateException("Could not locate an artist download with review source ID: ${source.id} and song name ${song.name}!")
			val videos = youtubeApiClient.findVideos("${song.artist} ${song.name}").videos

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

			val downloadDTO = YoutubeDownloadDTO(
					url = video.videoUrl,
					name = song.name,
					artist = song.artist,
					album = song.album,
					releaseYear = song.releaseYear,
					cropArtToSquare = true
			)

			val track = youtubeDownloadService.downloadSong(firstUser.first(), downloadDTO, storeArt = false)
			track.reviewSource = source
			track.inReview = true
			track.lastReviewed = now()
			trackRepository.save(track)

			artistDownload.lastDownloadAttempt = now()
			artistDownload.downloadedAt = artistDownload.lastDownloadAttempt
			reviewSourceArtistDownloadRepository.save(artistDownload)

			// Because we started from Spotify, we have a URL to the actual album art.
			// This is better than whatever it is we will get from the YT download, so
			// grab the art and store it
			imageService.downloadFromUrl(song.albumArtUrl!!)?.let { image ->
				songIngestionService.storeAlbumArtForTrack(image, track, false)
			}

			// The YT download service will save the Track for the user that downloads it.
			// So for every other user make a copy of that track
			otherUsers.forEach { otherUser ->
				trackService.saveTrackForUserReview(otherUser, track, source)
			}

			downloadCount++
		}

		return downloadCount
	}


	private fun YoutubeApiClient.YoutubeVideo.isValidForSong(song: MetadataResponseDTO): Boolean {
		val lowerTitle = this.title.toLowerCase()

		// Make sure the artist is in the title somewhere. If it isn't that seems like a bad sign
		if (!lowerTitle.contains(song.artist.toLowerCase())) {
			return false
		}

		// If the duration doesn't match closely with Spotify's expected duration, that's a bad sign
		if (this.duration < song.songLength - SORT_LENGTH_IDENTIFICATION_TOLERANCE ||
				this.duration > song.songLength + SORT_LENGTH_IDENTIFICATION_TOLERANCE) {
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

	fun subscribeToArtist(artistName: String): Pair<Boolean, List<String>> {
		val currentUser = loadLoggedInUser()

		// First, check if the artist is already subscribed to by someone else
		reviewSourceArtistRepository.findByArtistName(artistName)?.let { existing ->
			if (existing.isUserSubscribed(currentUser)) {
				throw IllegalArgumentException("User is already subscribed to artist $artistName!")
			}
			existing.subscribedUsers.add(currentUser)
			reviewSourceArtistRepository.save(existing)
			return true to emptyList()
		}

		// Ok so nobody has subscribed to this artist before. We need to create a new one and add our user to it

		// Start by finding the artists Spotify gives us based off the name
		val artists = spotifyApiClient.searchArtistsByName(artistName)

		// Try to find an exact match. If we don't, return a list of artists to try to help the end user
		// fix any typos or things of that nature
		val targetName = artistName.toLowerCase()
		val foundArtist = artists.find { it.name.toLowerCase() == targetName }
				?: return false to artists.map { it.name }.take(5)

		// Cool cool cool we found an artist in spotify. Now just save it.
		// Hard-code "searchNewerThan" to now() until we are not throttled by YouTube and can search unlimited
		val source = ReviewSourceArtist(artistId = foundArtist.id, artistName = foundArtist.name, searchNewerThan = now())
		source.subscribedUsers.add(currentUser)
		reviewSourceArtistRepository.save(source)

		return true to emptyList()
	}

	companion object {
		private val logger = logger()

		// When we are checking if a YouTube video is valid for a given Spotify song, we want to make sure
		// that the song lengths more or less agree. This is the tolerance for that check
		private const val SORT_LENGTH_IDENTIFICATION_TOLERANCE = 4
	}
}
