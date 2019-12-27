package com.example.groove.controllers

import com.example.groove.dto.TrackHistoryDTO
import com.example.groove.services.TrackHistoryService

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
					trackHistoryId = it.id,
					listenedDate = it.createdAt,
					trackLength = it.track.length,
					trackArtist = it.track.artist,
					trackAlbum = it.track.album,
					trackName = it.track.name
			)
		}
    }
}
