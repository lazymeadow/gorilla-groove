package com.example.groove.controllers

import com.example.groove.services.UserLibraryService
import com.example.groove.util.loadLoggedInUser

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("library")
class UserLibraryController @Autowired constructor(
		private val userLibraryService: UserLibraryService
) {

	@PostMapping
	fun addToLibrary(
			@RequestBody addToLibraryDTO: AddToLibraryDTO
	): ResponseEntity<String> {
		userLibraryService.addTrack(loadLoggedInUser(), addToLibraryDTO.trackId)

		return ResponseEntity
				.ok()
				.build()
	}

	data class AddToLibraryDTO(
			val trackId: Long
	)
}
