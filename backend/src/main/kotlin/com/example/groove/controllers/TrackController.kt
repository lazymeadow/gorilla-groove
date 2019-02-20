package com.example.groove.controllers

import com.example.groove.db.model.Track
import com.example.groove.dto.UpdateTrackDTO
import com.example.groove.dto.YoutubeDownloadDTO
import com.example.groove.services.TrackService
import com.example.groove.services.YoutubeService
import com.example.groove.util.loadLoggedInUser

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<Track> {
		return trackService.getTracks(name, artist, album, userId, searchTerm, pageable)
    }

	@PostMapping("/mark-listened")
	fun markSongAsListenedTo(@RequestBody markSongAsReadDTO: MarkTrackAsListenedToDTO): ResponseEntity<String> {
		trackService.markSongListenedTo(markSongAsReadDTO.trackId)

		return ResponseEntity(HttpStatus.OK)
	}

	@PostMapping("/set-hidden")
	fun setHidden(@RequestBody setHiddenDTO: SetHiddenDTO): ResponseEntity<String> {
		trackService.setHidden(setHiddenDTO.trackIds, setHiddenDTO.isHidden)

		return ResponseEntity(HttpStatus.OK)
	}

	@PostMapping("/import")
	fun importTracks(@RequestBody importDTO: MultiTrackIdDTO): List<Track> {
		return trackService.importTrack(importDTO.trackIds)
	}

	// FIXME this should be a PATCH not a PUT. But I was having issues with PATCH failing the OPTIONS check
	@PutMapping
	fun updateTrackData(@RequestBody updateTrackDTO: UpdateTrackDTO): ResponseEntity<String> {
		trackService.updateTracks(loadLoggedInUser(), updateTrackDTO)

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

	data class MarkTrackAsListenedToDTO(
			val trackId: Long
	)

	data class SetHiddenDTO(
			val trackIds: List<Long>,
			val isHidden: Boolean
	)

	data class MultiTrackIdDTO(
			val trackIds: List<Long>
	)


}
