package com.example.groove.services

import com.example.groove.db.model.Track
import com.example.groove.db.model.User
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.YouTubeDlProperties
import com.example.groove.util.createMapper
import com.example.groove.util.logger
import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import kotlin.math.abs


@Service
class YoutubeDownloadService(
		private val songIngestionService: SongIngestionService,
		private val fileStorageProperties: FileStorageProperties,
		private val youTubeDlProperties: YouTubeDlProperties,
		private val imageService: ImageService
) {

	val mapper = createMapper()

	fun downloadSong(user: User, youtubeDownloadDTO: YoutubeDownloadDTO): Track {
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
		if (!newSong.exists()) {
			throw YouTubeDownloadException("Failed to download song from URL: $url")
		}

		val videoArt = chooseAlbumArt(youtubeDownloadDTO.artUrl, destination) ?: run {
			logger.error("Failed to download album art for song at URL: $url!")
			null
		}

		// TODO Instead of passing the "url" in here, it would be cool to pass in the video title.
		// Slightly complicates finding the file after we save it, though
		val track = songIngestionService.convertAndSaveTrackForUser(youtubeDownloadDTO, user, newSong, videoArt, url)

		newSong.delete()

		return track
	}

	// An override link can be provided for a download if we're coming from Spotify. However, if the link
	// does not exist, or fails to download, fall back to the video thumbnail so we get something
	private fun chooseAlbumArt(overrideArtLink: String?, thumbnailArtPath: String): File? {
		val videoArt = findVideoArtOnDisk(thumbnailArtPath)

		return if (overrideArtLink != null) {
			val overrideArt = imageService.downloadFromUrl(overrideArtLink)

			if (overrideArt == null) {
				if (videoArt == null) {
					logger.error("Could not grab the override album art link ${overrideArtLink}! Using video thumbnail instead (if it exists)")
					null
				} else {
					logger.error("Could not grab the override album art link ${overrideArtLink}! The video thumbnail also could not be found!")
					videoArt
				}
			} else {
				videoArt?.delete()
				overrideArt
			}
		} else {
			videoArt
		}
	}

	private fun findVideoArtOnDisk(fileDestinationWithoutExtension: String): File? {
		listOf("webp", "jpg").forEach { extension ->
			val newAlbumArt = File("$fileDestinationWithoutExtension.$extension")
			if (newAlbumArt.exists()) {
				return newAlbumArt
			}
		}

		return null
	}

	// target length is used to try to find videos that closely match what is found in Spotify
	fun searchYouTube(searchTerm: String, targetLength: Int): List<VideoProperties> {
		logger.info("Searching YouTube for the term: '$searchTerm'")
		val videosToSearch = 5
		// This combination of arguments will have Youtube-DL not actually download the videos, but instead write the following
		// 3 flags to standard out, video ID, duration, and name. We redirect standard out to a file to process it after, in
		// order to read in this information and find the video we want to actually download
		val pb = ProcessBuilder(
				youTubeDlProperties.youtubeDlBinaryLocation + "youtube-dl",
				"ytsearch$videosToSearch:\"$searchTerm\"",
				"--skip-download",
				"--dump-json",
				"--no-cache-dir"
		)

		val tmpFileName = UUID.randomUUID().toString()
		val destination = fileStorageProperties.tmpDir + tmpFileName

		val outputFile = File(destination)
		pb.redirectOutput(outputFile)
		pb.redirectError(ProcessBuilder.Redirect.INHERIT)

		logger.info(pb.command().joinToString(" "))
		val p = pb.start()
		p.waitFor()

		val fileContent = outputFile.useLines { it.toList() }

		outputFile.delete()

		return fileContent.map { videoJson ->
			mapper.readValue(videoJson, VideoProperties::class.java)
		}.filter { videoProperties ->
			abs(videoProperties.duration - targetLength) < SONG_LENGTH_IDENTIFICATION_TOLERANCE
		}
	}

	@Suppress("unused")
	data class VideoProperties(
			@JsonAlias("display_id")
			val id: String,

			@JsonAlias("webpage_url")
			val videoUrl: String,

			val duration: Int,
			val title: String,

			@JsonAlias("uploader")
			val channelName: String = ""
	) {
		val embedUrl: String
			get() = "https://www.youtube.com/embed/$id"
	}

	companion object {
		val logger = logger()

		// When we are checking if a YouTube video is valid for a given Spotify song, we want to make sure
		// that the song lengths more or less agree. This is the tolerance for that check
		private const val SONG_LENGTH_IDENTIFICATION_TOLERANCE = 4
	}
}

class YouTubeDownloadException(error: String): RuntimeException(error)
