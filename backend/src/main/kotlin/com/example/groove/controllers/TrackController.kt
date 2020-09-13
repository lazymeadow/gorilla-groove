package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.dto.*
import com.example.groove.services.MetadataRequestService
import com.example.groove.services.TrackService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

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
		private val metadataRequestService: MetadataRequestService
) {

	//example: http://localhost:8080/api/track?page=0&size=1&sort=name,asc
	@GetMapping
	fun getTracks(
			@RequestParam("userId") userId: Long?,
			@RequestParam("name") name: String?,
			@RequestParam("artist") artist: String?,
			@RequestParam("album") album: String?,
			@RequestParam("searchTerm") searchTerm: String?,
			@RequestParam("showHidden") showHidden: Boolean?,
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

	// This endpoint is laughably narrow in scope. But I don't know for sure how this is going to evolve later
	@GetMapping("/all-count-since-timestamp")
	fun getAllTrackCountSinceTimestamp(
			@RequestParam("timestamp") unixTimestamp: Long
	): Int {
		return trackService.getAllTrackCountSinceTimestamp(Timestamp(unixTimestamp))
	}

	@PostMapping("/mark-listened")
	fun markSongAsListenedTo(
			@RequestBody markSongAsReadDTO: MarkTrackAsListenedToDTO,
			request: HttpServletRequest
	): ResponseEntity<String> {
		val user = loadLoggedInUser()
		val ipAddress = request.getHeader("x-forwarded-for")
		logger.info("User ${user.name} listened to track with ID: ${markSongAsReadDTO.trackId}")

		val deviceId = markSongAsReadDTO.deviceId
				?: user.currentAuthToken!!.device?.deviceId
				?: throw IllegalArgumentException("A device must be specified on either the request, or the logged in user's current token!")

		trackService.markSongListenedTo(markSongAsReadDTO.trackId, deviceId, ipAddress)

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
		val mapper = jacksonObjectMapper()
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

		return trackService.saveFromYoutube(youTubeDownloadDTO)
	}

	@PostMapping("/trim")
	fun trimSong(@RequestBody trackTrimDTO: TrackTrimDTO): Map<String, Int> {
		logger.info("User ${loadLoggedInUser().username} is attempting to trim a track: $trackTrimDTO")

		if (trackTrimDTO.startTime == null && trackTrimDTO.duration == null) {
			throw IllegalArgumentException("No trimming parameters were passed")
		}

		val regex = Regex("^[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{3})?\$")

		trackTrimDTO.startTime?.let {
			require(regex.matches(it)) { "Invalid startTime format!" }
		}
		trackTrimDTO.duration?.let {
			require(regex.matches(it)) { "Invalid endTime format!" }
		}

		val newLength = trackService.trimTrack(trackTrimDTO.trackId, trackTrimDTO.startTime, trackTrimDTO.duration)

		return mapOf("newLength" to newLength)
	}

	@PostMapping("/data-update-request")
	fun dataUpdateRequest(@RequestBody metadataUpdateRequestDTO: MetadataUpdateRequestDTO): DataUpdateResponseDTO {
		val (updatedTracks, failedTrackIds) =  metadataRequestService.requestTrackMetadata(metadataUpdateRequestDTO)

		return DataUpdateResponseDTO(
				successfulUpdates = updatedTracks,
				failedUpdateIds = failedTrackIds
		)
	}

	@GetMapping("/{trackId}")
	fun getTrack(@PathVariable trackId: Long): Track {
		return trackService.getTracksByIds(setOf(trackId)).first()
	}

	@GetMapping("/preview/public/{trackId}")
	fun getInfoForTrackAnonymous(@PathVariable trackId: Long): Any {
		return trackService.getPublicTrackInfo(trackId, true)
	}

	// It's real annoying that I have to have two endpoints for this, but I can't figure out
	// how to make Spring try to authenticate user for a public endpoint. So instead, have one
	// endpoint for authenticated users and one for not, and make the clients deal with it
	@GetMapping("/preview/{trackId}")
	fun getInfoForTrack(@PathVariable trackId: Long): Map<String, Any?> {
		return trackService.getPublicTrackInfo(trackId, false)
	}

	data class MarkTrackAsListenedToDTO(
			val trackId: Long,
			@Deprecated("This should not be getting passed in via this request going forward. The device ID is stored with the auth token and no longer needs to be")
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
		val logger = logger()
	}
}
