package com.example.groove.controllers

import com.example.groove.services.event.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("api/event")
class EventController(
		private val eventServiceCoordinator: EventServiceCoordinator
) {
	@GetMapping("/device-id/{device-id}")
	fun getEvents(
			@PathVariable("device-id") deviceId: String,
			@RequestParam("lastEventId") lastEventId: Int
	): ResponseEntity<EventResponse?> {
		eventServiceCoordinator.getEvent(deviceId = deviceId, lastEventId = lastEventId)?.let {
			return ResponseEntity.ok(it)
		}

		return ResponseEntity(HttpStatus.NOT_FOUND)
	}

	@PostMapping("/NOW_PLAYING")
	fun createNowPlayingEvent(@RequestBody body: NowPlayingEventRequest) {
		eventServiceCoordinator.sendEvent(EventType.NOW_PLAYING, body)
	}

	@PostMapping("/REMOTE_PLAY")
	fun createRemotePlayEvent(@RequestBody body: RemotePlayEventRequest) {
		eventServiceCoordinator.sendEvent(EventType.REMOTE_PLAY, body)
	}
}
