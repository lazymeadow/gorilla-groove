package com.example.groove.controllers

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.dto.UpdateTrackDTO
import com.example.groove.services.TrackService
import com.example.groove.util.loadLoggedInUser

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/track")
class TrackController(
		private val trackService: TrackService,
		private val trackRepository: TrackRepository
) {

	//example: http://localhost:8080/api/track?page=0&size=1&sort=name,asc
	@GetMapping
    fun getTracks(
			@RequestParam(value = "userId") userId: Long?,
			@RequestParam(value = "name") name: String?,
			@RequestParam(value = "artist") artist: String?,
			@RequestParam(value = "album") album: String?,
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<Track> {
		return trackService.getTracks(name, artist, album, userId, pageable)
    }

	// I don't know that this still makes sense. I think the two ways to add tracks
	// will be uploading them, and cloning them from another user. Probably both will
	// have dedicated endpoints
//	@PostMapping
//	fun addToLibrary(@RequestBody addToLibraryDTO: AddToLibraryDTO): ResponseEntity<String> {
//		trackService.addTrack(loadLoggedInUser(), addToLibraryDTO.trackId)
//
//		return ResponseEntity
//				.ok()
//				.build()
//	}

	@PostMapping("/mark-listened")
	fun markSongAsListenedTo(@RequestBody markSongAsReadDTO: MarkTrackAsListenedToDTO): ResponseEntity<String> {
		trackService.markSongListenedTo(markSongAsReadDTO.trackId)
		return ResponseEntity(HttpStatus.OK)
	}

	@Transactional
	@PostMapping("/set-hidden")
	fun setHidden(@RequestBody setHiddenDTO: SetHiddenDTO): ResponseEntity<String> {
		trackRepository.setHiddenForUser(setHiddenDTO.trackIds, loadLoggedInUser().id, setHiddenDTO.isHidden)

		return ResponseEntity(HttpStatus.OK)
	}

	// FIXME this should be a PATCH not a PUT. But I was having issues with PATCH failing the OPTIONS check
	@PutMapping
	fun updateTrackData(@RequestBody updateTrackDTO: UpdateTrackDTO): ResponseEntity<String> {
		trackService.updateTrack(loadLoggedInUser(), updateTrackDTO)

		return ResponseEntity(HttpStatus.OK)
	}

	data class AddToLibraryDTO(
			val trackId: Long
	)

	data class MarkTrackAsListenedToDTO(
			val trackId: Long
	)

	data class SetHiddenDTO(
			val trackIds: List<Long>,
			val isHidden: Boolean
	)

}
