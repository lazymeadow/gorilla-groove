package com.example.groove.controllers

import com.example.groove.dto.CurrentlyListeningUsersDTO
import com.example.groove.services.CurrentlyListeningService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val CHECK_INTERVAL = 2000L
private const val NUM_CHECKS = 5

@RestController("api/currently-listening")
class CurrentlyListeningController(
		private val currentlyListeningService: CurrentlyListeningService
) {

	@GetMapping
	fun getCurrentlyListening(
			@RequestParam("lastUpdate") lastUpdate: Int
	): ResponseEntity<CurrentlyListeningUsersDTO?> {
		val currentUser = loadLoggedInUser()

		// Long poll for updates to the currently listening
		for (i in 0..NUM_CHECKS) {
			val result = currentlyListeningService.getListeningUsersIfNew(currentUser, lastUpdate)

			if (result != null) {
				return ResponseEntity.ok(result)
			} else {
				Thread.sleep(CHECK_INTERVAL)
			}
		}

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(CurrentlyListeningUsersDTO(
						currentlyListeningUsers = null,
						lastUpdate = lastUpdate
				))
	}

	@PostMapping
	fun setCurrentlyListening(@RequestBody body: NewCurrentlyListening) {
		logger.info("Set currently listening to ${body.song}")
		currentlyListeningService.setListeningUser(loadLoggedInUser(), body.song)
	}

	data class NewCurrentlyListening(val song: String)

	companion object {
		private val logger = logger<CurrentlyListeningService>()
	}
}