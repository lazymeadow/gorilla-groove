package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.db.model.User
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.YouTubeDlProperties
import com.example.groove.util.createMapper
import com.example.groove.util.logger
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.*


@Service
class YoutubeDownloadService(
		private val songIngestionService: SongIngestionService,
		private val fileStorageProperties: FileStorageProperties,
		private val trackRepository: TrackRepository,
		private val youTubeDlProperties: YouTubeDlProperties
) {

	val mapper = createMapper()

	// Currently an issue with downloading thumbnails from YT
	// https://github.com/ytdl-org/youtube-dl/issues/25684
	@Transactional
	fun downloadSong(user: User, youtubeDownloadDTO: YoutubeDownloadDTO, storeArt: Boolean = true): Track {
		val url = youtubeDownloadDTO.url

		val tmpFileName = UUID.randomUUID().toString()
		val destination = fileStorageProperties.tmpDir + tmpFileName

		val pb = ProcessBuilder(
				youTubeDlProperties.youtubeDlBinaryLocation + "youtube-dl",
				url,
				"--extract-audio",
				"--audio-format",
				"vorbis",
				"-o",
				"$destination.ogg",
				"--no-cache-dir", // Ran into issues with YT giving us 403s unless we cleared cache often, so just ignore cache
				"--write-thumbnail" // this started to plop things out as .webp. No way to pick the format right now and webp breaks image format conversion currently
		)
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		pb.redirectError(ProcessBuilder.Redirect.INHERIT)
		val p = pb.start()
		p.waitFor()

		val newSong = File("$destination.ogg")
		val newAlbumArt = findArt(destination)
		if (!newSong.exists()) {
			throw YouTubeDownloadException("Failed to download song from URL: $url")
		}

		// TODO Instead of passing the "url" in here, it would be cool to pass in the video title.
		// Slightly complicates finding the file after we save it, though
		val track = songIngestionService.convertAndSaveTrackForUser(newSong, user, url, hasArt = newAlbumArt != null)
		// If the uploader provided any metadata, add it to the track and save it again
		youtubeDownloadDTO.name?.let { track.name = it.trim() }
		youtubeDownloadDTO.artist?.let { track.artist = it.trim() }
		youtubeDownloadDTO.featuring?.let { track.featuring = it.trim() }
		youtubeDownloadDTO.album?.let { track.album = it.trim() }
		youtubeDownloadDTO.releaseYear?.let { track.releaseYear = it }
		youtubeDownloadDTO.trackNumber?.let { track.trackNumber = it }
		youtubeDownloadDTO.genre?.let { track.genre = it.trim() }
		trackRepository.save(track)

		if (!storeArt) {
			newAlbumArt?.delete()
		} else if (newAlbumArt != null) {
			songIngestionService.storeAlbumArtForTrack(newAlbumArt, track, youtubeDownloadDTO.cropArtToSquare)
			newAlbumArt.delete()
		} else {
			logger.error("Failed to download album art for song at URL: $url.")
		}

		newSong.delete()

		return track
	}

	private fun findArt(fileDestinationWithoutExtension: String): File? {
		listOf("webp", "jpg").forEach { extension ->
			val newAlbumArt = File("$fileDestinationWithoutExtension.$extension")
			if (newAlbumArt.exists()) {
				return newAlbumArt
			}
		}

		return null
	}

	fun searchYouTube(searchTerm: String): List<VideoProperties> {
		logger.info("Searching YouTube for the term: '$searchTerm'")
		val videosToSearch = 5
		// This combination of arguments will have Youtube-DL not actually download the videos, but instead write the following
		// 3 flags to standard out, video ID, duration, and name. We redirect standard out to a file to process it after, in
		// order to read in this information and find the video we want to actually download
		val pb = ProcessBuilder(
				youTubeDlProperties.youtubeDlBinaryLocation + "youtube-dl",
				"ytsearch$videosToSearch:$searchTerm",
				"--skip-download",
				"--dump-json",
				"--no-cache-dir"
		)

		val tmpFileName = UUID.randomUUID().toString()
		val destination = fileStorageProperties.tmpDir + tmpFileName

		val outputFile = File(destination)
		pb.redirectOutput(outputFile)
		pb.redirectError(ProcessBuilder.Redirect.INHERIT)
		val p = pb.start()
		p.waitFor()

		val fileContent = outputFile.useLines { it.toList() }

		return fileContent.map { videoJson ->
			mapper.readValue(videoJson, VideoProperties::class.java)
		}
	}

	data class VideoProperties(
			@JsonProperty("webpage_url")
			val videoUrl: String,
			val duration: Int,
			val title: String,
			@JsonProperty("uploader")
			val channelName: String = ""
	)

	companion object {
		val logger = logger()
	}
}

class YouTubeDownloadException(error: String): RuntimeException(error)
