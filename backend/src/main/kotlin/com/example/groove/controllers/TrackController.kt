package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.dto.*
import com.example.groove.services.MetadataRequestService
import com.example.groove.services.TrackService
import com.example.groove.services.enums.AudioFormat
import com.example.groove.util.createMapper
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.time.ZonedDateTime
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
			@RequestParam("excludedPlaylistId") excludedPlaylistId: Long?,
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<Track> {
		return trackService.getTracks(
				name = name,
				artist = artist,
				album = album,
				userId = userId,
				searchTerm = searchTerm,
				showHidden = showHidden ?: false,
				excludedPlaylistId = excludedPlaylistId,
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
			@RequestBody markSongListenedDTO: MarkTrackAsListenedToDTO,
			request: HttpServletRequest
	): ResponseEntity<String> {
		val user = loadLoggedInUser()
		val ipAddress = request.getHeader("x-forwarded-for")
		logger.info("User ${user.name} listened to track with ID: ${markSongListenedDTO.trackId} at ${markSongListenedDTO.timeListenedAt}")

		val deviceId = markSongListenedDTO.deviceId
				?: user.currentAuthToken!!.device?.deviceId
				?: throw IllegalArgumentException("A device must be specified on either the request, or the logged in user's current token!")

		// Clients pass in the time that they listened to something (mostly so that offline listening on mobile can still
		// report an accurate play history when they go back online). This is just a sanity check to make sure that plays
		// aren't being recorded in the future. The 2 minute buffer is built in to give leniency for devices having a differing clock
		markSongListenedDTO.timeListenedAt.toInstant().toEpochMilli().let { clientMillis ->
			val serverMillis = System.currentTimeMillis() + 120_000
			require (clientMillis < serverMillis) {
				"You may not mark a song as listened to in the future! Your epoch millis was recorded as $clientMillis. It must be less than $serverMillis"
			}
		}

		trackService.markSongListenedTo(
				deviceId = deviceId,
				remoteIp = ipAddress,
				data = markSongListenedDTO
		)

		return ResponseEntity(HttpStatus.OK)
	}

	// I don't really remember why I made this a separate thing, but whatever reason that was never materialized.
	// Don't use this. Instead just use the normal metadata editing endpoints that now support setting private-ness
	@PostMapping("/set-private")
	fun setPrivate(@RequestBody setPrivateDTO: SetPrivateDTO): ResponseEntity<String> {
		trackService.setPrivate(setPrivateDTO.trackIds, setPrivateDTO.isPrivate)

		return ResponseEntity(HttpStatus.OK)
	}

	@PostMapping("/import")
	fun importTracks(@RequestBody importDTO: MultiTrackIdDTO): List<Track> {
		return trackService.importTrack(importDTO.trackIds)
	}

	// The other update track endpoint is more difficult to use as it allows you to upload binary album art data within
	// the request. This endpoint does the same update (minus album art capabilities) and is simpler to consume from
	@PutMapping("/simple-update")
	fun updateTrackDataNoAlbumArt(@RequestBody updateTrackDTO: UpdateTrackDTO): TrackUpdateResponse {
		val tracks = trackService.updateTracks(loadLoggedInUser(), updateTrackDTO, null)

		return TrackUpdateResponse(items = tracks)
	}

	// Can't seem to deserialize a multipart file alongside other data using @RequestBody. So this is my dumb solution
	@PutMapping
	fun updateTrackData(
			@RequestParam("albumArt") albumArt: MultipartFile?,
			@RequestParam("updateTrackJson") updateTrackJson: String
	): ResponseEntity<String> {
		val mapper = createMapper()
		val updateTrackDTO = mapper.readValue(updateTrackJson, UpdateTrackDTO::class.java)

		if (albumArt != null && updateTrackDTO.albumArtUrl != null) {
			throw IllegalArgumentException("It is invalid to supply albumArt binary data and an albumArtUrl. Which one would we use?")
		}

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

		return trackService.saveFromYoutube(youTubeDownloadDTO, loadLoggedInUser())
	}

	@PostMapping("/trim")
	fun trimSong(@RequestBody trackTrimDTO: TrackTrimDTO): Map<String, Int> {
		logger.info("User ${loadLoggedInUser().name} is attempting to trim a track: $trackTrimDTO")

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

	data class VolumeAdjustDTO(val volumeAdjustAmount: Double)

	@PostMapping("/{trackId}/volume-adjust")
	fun adjustVolume(
			@PathVariable trackId: Long,
			@RequestBody volumeAdjustDTO: VolumeAdjustDTO
	) {
		logger.info("User ${loadLoggedInUser().name} is editing the volume of a track $trackId by ${volumeAdjustDTO.volumeAdjustAmount}")

		require(volumeAdjustDTO.volumeAdjustAmount > 0) {
			"Volume adjustment must be greater than 0"
		}

		require(volumeAdjustDTO.volumeAdjustAmount != 1.0) {
			"Adjusting the volume to 1.0 would not do anything"
		}

		trackService.adjustVolume(trackId, volumeAdjustDTO.volumeAdjustAmount)
	}

	@PostMapping("/data-update-request")
	fun dataUpdateRequest(@RequestBody metadataUpdateRequestDTO: MetadataUpdateRequestDTO): DataUpdateResponseDTO {
		val (updatedTracks, failedTrackIds) =  metadataRequestService.requestTrackMetadata(metadataUpdateRequestDTO, loadLoggedInUser())

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
	fun getInfoForTrackAnonymous(
			@PathVariable trackId: Long,
			@RequestParam(defaultValue = "OGG") audioFormat: AudioFormat
	): Map<String, Any?> {
		logger.info("Public track info requested for track $trackId, format $audioFormat")
		return trackService.getPublicTrackInfo(trackId, true, audioFormat)
	}

	// It's real annoying that I have to have two endpoints for this, but I can't figure out
	// how to make Spring try to authenticate user for a public endpoint. So instead, have one
	// endpoint for authenticated users and one for not, and make the clients deal with it
	@GetMapping("/preview/{trackId}")
	fun getInfoForTrack(
			@PathVariable trackId: Long,
			@RequestParam(defaultValue = "OGG") audioFormat: AudioFormat
	): Map<String, Any?> {
		logger.info("Private track info requested for track $trackId, format $audioFormat by user ${loadLoggedInUser().name}")
		return trackService.getPublicTrackInfo(trackId, false, audioFormat)
	}

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

data class MarkTrackAsListenedToDTO(
		val trackId: Long,
		@Deprecated("This should not be getting passed in via this request going forward. The device ID is stored with the auth token and no longer needs to be")
		val deviceId: String?,
		val timeListenedAt: ZonedDateTime = ZonedDateTime.now(),
		val ianaTimezone: String = "America/Boise",
		val latitude: Double?,
		val longitude: Double?
)

data class TrackUpdateResponse(
		val items: List<Track>
)
