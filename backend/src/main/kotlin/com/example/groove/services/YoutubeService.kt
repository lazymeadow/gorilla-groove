package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.properties.FileStorageProperties
import com.example.groove.properties.YouTubeDlProperties
import com.example.groove.util.loadLoggedInUser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.*


@Service
class YoutubeService(
		private val songIngestionService: SongIngestionService,
		private val fileStorageProperties: FileStorageProperties,
		private val trackRepository: TrackRepository,
		private val youTubeDlProperties: YouTubeDlProperties,
		private val fileStorageService: FileStorageService
) {

	@Transactional
	fun downloadSong(youtubeDownloadDTO: YoutubeDownloadDTO): Track {
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
		if (!newAlbumArt.exists()) {
			logger.error("Failed to download album art for song at URL: $url. Continuing anyway")
		}

		val track = songIngestionService.convertAndSaveTrackForUser("$tmpFileName.ogg", loadLoggedInUser())
		// If the uploader provided any metadata, add it to the track and save it again
		youtubeDownloadDTO.name?.let { track.name = it }
		youtubeDownloadDTO.artist?.let { track.artist = it }
		youtubeDownloadDTO.featuring?.let { track.featuring = it }
		youtubeDownloadDTO.album?.let { track.album = it }
		youtubeDownloadDTO.releaseYear?.let { track.releaseYear = it }
		youtubeDownloadDTO.trackNumber?.let { track.trackNumber = it }
		youtubeDownloadDTO.genre?.let { track.genre = it }
		trackRepository.save(track)

		// TODO convert this to a png like the others
		fileStorageService.storeAlbumArt(newAlbumArt, track.id)

		// We have stored the file in its permanent home. We can delete this tmp file
		newAlbumArt.delete()

		return track
	}

	companion object {
		val logger = LoggerFactory.getLogger(YoutubeService::class.java)!!
	}
}

class YouTubeDownloadException(error: String): RuntimeException(error)
