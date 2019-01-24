package com.example.groove.services

import com.example.groove.db.dao.TrackRepository
import com.example.groove.dto.YouTubeDownloadDTO
import com.example.groove.properties.FileStorageProperties
import com.example.groove.util.loadLoggedInUser
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File


@Service
class YouTubeService(
		private val fileStorageService: FileStorageService,
		private val fileStorageProperties: FileStorageProperties,
		private val trackRepository: TrackRepository
) {

	@Transactional(readOnly = true)
	fun downloadSong(youTubeDownloadDTO: YouTubeDownloadDTO) {
		val url = youTubeDownloadDTO.url

		val tmpFileName = RandomStringUtils.random(10)
		val destination = fileStorageProperties.uploadDir + "$tmpFileName.ogg"
		val command = "youtube-dl --extract-audio $url -o $destination --write-thumbnail"

		Runtime.getRuntime().exec(command)
		val newSong = File("$destination.ogg")
		val newAlbumArt = File("$destination.jpg")
		if (!newSong.exists()) {
			throw YouTubeDownloadException("Failed to download song from URL: $url")
		}
		if (!newAlbumArt.exists()) {
			logger.error("Failed to download album art for song at URL: $url. Continuing anyway")
		}

		val track = fileStorageService.convertAndSaveTrackForUser(tmpFileName, loadLoggedInUser())
		// If the uploader provided any metadata, add it to the track and save it again
		youTubeDownloadDTO.name?.let { track.name = it }
		youTubeDownloadDTO.artist?.let { track.artist = it }
		youTubeDownloadDTO.album?.let { track.album = it }
		trackRepository.save(track)

		// TODO convert this to a png like the others
		fileStorageService.moveAlbumArt(newAlbumArt, track.id)

		// TODO cleanup tmp files?
	}

	companion object {
		val logger = LoggerFactory.getLogger(YouTubeService::class.java)!!
	}
}

class YouTubeDownloadException(error: String): RuntimeException(error)
