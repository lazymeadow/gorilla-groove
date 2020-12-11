package com.example.groove.controllers

import com.example.groove.dto.TrackHistoryDTO
import com.example.groove.services.TrackHistoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*
import java.sql.Timestamp

@RestController
@RequestMapping("api/track-history")
class TrackHistoryController(
		private val trackHistoryService: TrackHistoryService
) {

	@GetMapping
    fun getTrackHistory(
			@RequestParam(value = "userId") userId: Long?,
			@RequestParam(value = "startDate") startDate: Long, // Unix timestamps
			@RequestParam(value = "endDate") endDate: Long
	): List<TrackHistoryDTO> {
		if (startDate > endDate) {
			throw IllegalArgumentException("Start date must not come after end date")
		}

		return trackHistoryService.getTrackHistory(userId, Timestamp(startDate), Timestamp(endDate)).map {
			TrackHistoryDTO(
					id = it.id,
					listenedDate = it.utcListenedAt,
					localListenDate = it.localTimeListenedAt,
					trackLength = it.track.length,
					trackArtist = it.track.artist,
					trackAlbum = it.track.album,
					trackName = it.track.name,
					deviceName = it.device?.deviceName
			)
		}
    }

	@DeleteMapping("/{id}")
    fun deleteTrackHistory(
			@PathVariable(value = "id") id: Long
	): ResponseEntity<String> {
		trackHistoryService.deleteTrackHistory(id)

		return ResponseEntity(HttpStatus.OK)
    }
}
