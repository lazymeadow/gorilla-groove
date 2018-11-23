package com.example.groove.controllers

import com.example.groove.db.dao.UserLibraryRepository
import com.example.groove.db.model.UserLibrary
import com.example.groove.services.UserLibraryService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.util.*

@RestController
@RequestMapping("library")
class UserLibraryController @Autowired constructor(
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
		// The user can pass in an ID of the library they want to view. If omitted, it'll just use their own
		val idToLoad = userId ?: loadLoggedInUser().id

		// This needs to handle hidden tracks depending on if you are logged in as the userId you're using
		// Likely should also move into userLibraryService
		return userLibraryRepository.getLibrary(name, artist, album, idToLoad, pageable)
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
	fun markTrackAsListenedTo(@RequestBody markTrackAsReadDTO: MarkTrackAsListenedToDTO): ResponseEntity<String> {
		val userLibraryTrack = userLibraryRepository.findById(markTrackAsReadDTO.userLibraryId).unwrap()

		if (userLibraryTrack == null || userLibraryTrack.user != loadLoggedInUser()) {
			throw IllegalArgumentException("No track found by ID ${markTrackAsReadDTO.userLibraryId}!")
		}

		// May want to do some sanity checks / server side validation here to prevent this incrementing too often.
		// We know the last played date of a track and can see if it's even possible to have listened to this song again
		userLibraryTrack.playCount++
		userLibraryTrack.lastPlayed = Timestamp(Date().time)

		return ResponseEntity(HttpStatus.OK)
	}

	data class AddToLibraryDTO(
			val trackId: Long
	)

	data class MarkTrackAsListenedToDTO(
			val userLibraryId: Long
	)
}
