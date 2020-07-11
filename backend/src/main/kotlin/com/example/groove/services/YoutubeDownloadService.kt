package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.db.model.User
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.YouTubeDlProperties
import com.example.groove.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.*


@Service
class YoutubeDownloadService(
		private val songIngestionService: SongIngestionService,
		private val fileStorageProperties: FileStorageProperties,
		private val trackRepository: TrackRepository,
		private val youTubeDlProperties: YouTubeDlProperties,
		private val fileStorageService: FileStorageService,
		private val imageService: ImageService
) {

	@Transactional
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
				"--write-thumbnail" // Seems to plop things out as .pngs in my testing. May need to convert it if not always PNGs
		)
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
		pb.redirectError(ProcessBuilder.Redirect.INHERIT)
		val p = pb.start()
		p.waitFor()

		val newSong = File("$destination.ogg")
		val newAlbumArt = File("$destination.jpg")
		if (!newSong.exists()) {
			throw YouTubeDownloadException("Failed to download song from URL: $url")
		}

		// TODO Instead of passing the "url" in here, it would be cool to pass in the video title.
		// Slightly complicates finding the file after we save it, though
		val track = songIngestionService.convertAndSaveTrackForUser(newSong, user, url)
		// If the uploader provided any metadata, add it to the track and save it again
		youtubeDownloadDTO.name?.let { track.name = it.trim() }
		youtubeDownloadDTO.artist?.let { track.artist = it.trim() }
		youtubeDownloadDTO.featuring?.let { track.featuring = it.trim() }
		youtubeDownloadDTO.album?.let { track.album = it.trim() }
		youtubeDownloadDTO.releaseYear?.let { track.releaseYear = it }
		youtubeDownloadDTO.trackNumber?.let { track.trackNumber = it }
		youtubeDownloadDTO.genre?.let { track.genre = it.trim() }
		trackRepository.save(track)

		if (newAlbumArt.exists()) {
			if (youtubeDownloadDTO.cropArtToSquare) {
				val croppedArt = imageService.cropToSquare(newAlbumArt)
				fileStorageService.storeAlbumArt(croppedArt, track.id)
				croppedArt.delete()
			} else {
				fileStorageService.storeAlbumArt(newAlbumArt, track.id)
			}

			newAlbumArt.delete()
		} else {
			logger.error("Failed to download album art for song at URL: $url.")
		}

		newSong.delete()

		return track
	}

	companion object {
		val logger = logger()
	}
}

class YouTubeDownloadException(error: String): RuntimeException(error)
