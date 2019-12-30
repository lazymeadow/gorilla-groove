package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.dto.TrackChangesDTO
import com.example.groove.dto.TrackTrimDTO
import com.example.groove.dto.UpdateTrackDTO
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.services.TrackService
import com.example.groove.services.YoutubeService
import com.example.groove.util.loadLoggedInUser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("api/track")
class TrackController(
		private val trackService: TrackService,
		private val youTubeService: YoutubeService
) {

	//example: http://localhost:8080/api/track?page=0&size=1&sort=name,asc
	@GetMapping
    fun getTracks(
			@RequestParam(value = "userId") userId: Long?,
			@RequestParam(value = "name") name: String?,
			@RequestParam(value = "artist") artist: String?,
			@RequestParam(value = "album") album: String?,
			@RequestParam(value = "searchTerm") searchTerm: String?,
			@RequestParam(value = "showHidden") showHidden: Boolean?,
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<Track> {
		return trackService.getTracks(
				name = name,
				artist = artist,
				album = album,
				userId = userId,
				searchTerm = searchTerm,
				showHidden = showHidden ?: false,
				pageable = pageable
		)
    }

	// Used by the Android App
	@GetMapping("/changes-since-timestamp/userId/{userId}/timestamp/{timestamp}")
    fun getTracksChangedSinceTimestamp(
			@PathVariable(value = "userId") userId: Long,
			@PathVariable(value = "timestamp") unixTimestamp: Long
	): TrackChangesDTO {
		return trackService.getTracksUpdatedSinceTimestamp(userId, Timestamp(unixTimestamp))
    }

	// This endpoint is laughably narrow in scope. But I don't know for sure how this is going to evolve later
	@GetMapping("/all-count-since-timestamp")
    fun getAllTrackCountSinceTimestamp(
			@RequestParam(value = "timestamp") unixTimestamp: Long
	): Int {
		return trackService.getAllTrackCountSinceTimestamp(Timestamp(unixTimestamp))
    }

	@PostMapping("/mark-listened")
	fun markSongAsListenedTo(
			@RequestBody markSongAsReadDTO: MarkTrackAsListenedToDTO,
			request: HttpServletRequest
	): ResponseEntity<String> {
		val ipAddress = request.getHeader("x-forwarded-for")
		if (markSongAsReadDTO.deviceId == null) {
			logger.warn("User ${loadLoggedInUser().name} listened to a song without the device included")
		}
		logger.info("User ${loadLoggedInUser().name} listened to track with ID: ${markSongAsReadDTO.trackId}")

		trackService.markSongListenedTo(markSongAsReadDTO.trackId, markSongAsReadDTO.deviceId, ipAddress)

		return ResponseEntity(HttpStatus.OK)
	}

	@PostMapping("/set-private")
	fun setPrivate(@RequestBody setPrivateDTO: SetPrivateDTO): ResponseEntity<String> {
		trackService.setPrivate(setPrivateDTO.trackIds, setPrivateDTO.isPrivate)

		return ResponseEntity(HttpStatus.OK)
	}

	@PostMapping("/import")
	fun importTracks(@RequestBody importDTO: MultiTrackIdDTO): List<Track> {
		return trackService.importTrack(importDTO.trackIds)
	}

	// FIXME this should be a PATCH not a PUT. But I was having issues with PATCH failing the OPTIONS check
	// Can't seem to deserialize a multipart file alongside other data using @RequestBody. So this is my dumb solution
	@PutMapping
	fun updateTrackData(
			@RequestParam("albumArt") albumArt: MultipartFile?,
			@RequestParam("updateTrackJson") updateTrackJson: String
	): ResponseEntity<String> {
		val mapper = ObjectMapper().registerKotlinModule()
		val updateTrackDTO = mapper.readValue(updateTrackJson, UpdateTrackDTO::class.java)

		trackService.updateTracks(loadLoggedInUser(), updateTrackDTO, albumArt)

		return ResponseEntity(HttpStatus.OK)
	}

	@DeleteMapping
	fun deleteTracks(@RequestBody deleteTrackDTO: MultiTrackIdDTO): ResponseEntity<String> {
		trackService.deleteTracks(loadLoggedInUser(), deleteTrackDTO.trackIds)

		return ResponseEntity(HttpStatus.OK)
	}

	@PostMapping("/youtube-dl")
	fun youtubeDownload(@RequestBody youTubeDownloadDTO: YoutubeDownloadDTO): Track {
		if (youTubeDownloadDTO.url.contains("&list")) {
			throw IllegalArgumentException("Playlist downloads are not allowed")
		}

		return youTubeService.downloadSong(youTubeDownloadDTO)
	}

	@PostMapping("/trim")
	fun trimSong(@RequestBody trackTrimDTO: TrackTrimDTO): Map<String, Int> {
		if (trackTrimDTO.startTime == null && trackTrimDTO.duration == null) {
			throw IllegalArgumentException("No trimming parameters were passed")
		}

		val regex = Regex("^[0-9]{2}:[0-9]{2}(\\.[0-9]{3})?\$")

		trackTrimDTO.startTime?.let {
			regex.matches(it)
		}

		trackTrimDTO.duration?.let {
			regex.matches(it)
		}

		val newLength = trackService.trimTrack(trackTrimDTO.trackId, trackTrimDTO.startTime, trackTrimDTO.duration)

		return mapOf("newLength" to newLength)
	}

	@GetMapping("/public/{trackId}")
	fun getLinksForTrackAnonymous(@PathVariable trackId: Long): Map<String, Any?> {
		return trackService.getPublicTrackInfo(trackId)
	}

	data class MarkTrackAsListenedToDTO(
			val trackId: Long,
			@Deprecated("'id' should not be null after clients are updated")
			val deviceId: String?
	)

	data class SetPrivateDTO(
			val trackIds: List<Long>,
			val isPrivate: Boolean
	)

	data class MultiTrackIdDTO(
			val trackIds: List<Long>
	)

	companion object {
		val logger = LoggerFactory.getLogger(TrackController::class.java)!!
	}
}
