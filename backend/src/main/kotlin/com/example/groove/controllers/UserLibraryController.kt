package com.example.groove.controllers

import com.example.groove.db.dao.UserLibraryRepository
import com.example.groove.db.model.UserLibrary
import com.example.groove.services.UserLibraryService
import com.example.groove.util.loadLoggedInUser

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("library")
class UserLibraryController(
		private val userLibraryService: UserLibraryService,
		private val userLibraryRepository: UserLibraryRepository
) {

	//example: http://localhost:8080/api/library?page=0&size=1&sort=name,asc
    @Transactional(readOnly = true)
	@GetMapping
    fun getTracks(
			@RequestParam(value = "userId") userId: Long?,
			@RequestParam(value = "name") name: String?,
			@RequestParam(value = "artist") artist: String?,
			@RequestParam(value = "album") album: String?,
			pageable: Pageable // The page is magic, and allows the frontend to use 3 optional params: page, size, and sort
	): Page<UserLibrary> {
		return userLibraryService.getUserLibrary(name, artist, album, userId, pageable)
    }

	@PostMapping
	fun addToLibrary(@RequestBody addToLibraryDTO: AddToLibraryDTO): ResponseEntity<String> {
		userLibraryService.addTrack(loadLoggedInUser(), addToLibraryDTO.trackId)

		return ResponseEntity
				.ok()
				.build()
	}

	@Transactional
	@PostMapping("/mark-listened")
	fun markSongAsListenedTo(@RequestBody markSongAsReadDTO: MarkTrackAsListenedToDTO): ResponseEntity<String> {
		userLibraryService.markSongListenedTo(markSongAsReadDTO.userLibraryId)
		return ResponseEntity(HttpStatus.OK)
	}

	@Transactional
	@PostMapping("/set-hidden")
	fun setHidden(@RequestBody setHiddenDTO: SetHiddenDTO): ResponseEntity<String> {
		userLibraryRepository.setHiddenForUser(setHiddenDTO.userLibraryIds, loadLoggedInUser().id, setHiddenDTO.isHidden)

		return ResponseEntity(HttpStatus.OK)
	}

	data class AddToLibraryDTO(
			val trackId: Long
	)

	data class MarkTrackAsListenedToDTO(
			val userLibraryId: Long
	)

	data class SetHiddenDTO(
			val userLibraryIds: List<Long>,
			val isHidden: Boolean
	)
}
