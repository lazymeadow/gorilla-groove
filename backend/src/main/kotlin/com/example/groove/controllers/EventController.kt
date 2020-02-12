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
			@PathVariable("device-id") deviceId: String?,
			@RequestParam("lastEventId") lastEventId: Int
	): ResponseEntity<EventResponse?> {
		eventServiceCoordinator.getEvent(deviceId = deviceId, lastUpdateId = lastEventId)?.let {
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


@Deprecated("Use EventController once clients are updated")
@RestController
@RequestMapping("api/currently-listening")
class CurrentlyListeningController(
		private val eventController: EventController
) {
	@GetMapping
	fun getCurrentlyListening(
			@RequestParam("lastUpdate") lastUpdate: Int
	): ResponseEntity<TemporaryNowPlayingEvent?> {
		val nowPlayingEvent = eventController.getEvents(deviceId = null, lastEventId = lastUpdate).body

		nowPlayingEvent as NowPlayingEventResponse?

		nowPlayingEvent?.let {
			return ResponseEntity.ok(TemporaryNowPlayingEvent(
					currentlyListeningUsers = nowPlayingEvent.currentlyListeningUsers,
					lastUpdate = nowPlayingEvent.lastUpdateId
			))
		}

		return ResponseEntity(HttpStatus.NOT_FOUND)
	}

	data class TemporaryNowPlayingEvent(
		val currentlyListeningUsers: Map<Long, SongListenResponse>?,
		val lastUpdate: Int
	)

	@PostMapping
	fun setCurrentlyListening(@RequestBody body: NewCurrentlyListening) {
		eventController.createNowPlayingEvent(NowPlayingEventRequest(
				trackId = body.trackId,
				deviceId = body.deviceId
		))
	}

	data class NewCurrentlyListening(
			val trackId: Long?, // Null when we stop listening to stuff

			val deviceId: String
	)
}
