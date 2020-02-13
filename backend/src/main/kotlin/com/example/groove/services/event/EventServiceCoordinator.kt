package com.example.groove.services.event

import org.springframework.stereotype.Service


@Service
class EventServiceCoordinator(
		private val eventServices: List<EventService>
) {

	fun getEvent(deviceId: String?, lastEventId: Int): EventResponse? {
		// Long poll for updates to events.
		// The first time we see a new event we will return
		for (i in 0..NUM_CHECKS) {
			eventServices.forEach { eventServices ->
				eventServices.getEvent(deviceId, lastEventId)?.let {
					// Kind of jank. Probably a better way to do this, but song listen events use an ID
					// to keep track of when to update you, and others don't. If we weren't using an event
					// service that cared about setting the lastEventId, then just use the one we started with
					if (it !is NowPlayingEventResponse) {
						it.lastEventId = lastEventId
					}
					return it
				}
			}

			Thread.sleep(CHECK_INTERVAL)
		}

		return null
	}

	fun sendEvent(eventType: EventType, event: EventRequest) {
		val service = eventServices.find { it.handlesEvent(eventType) }!!

		service.sendEvent(event)
	}

	companion object {
		private const val CHECK_INTERVAL = 2000L
		private const val NUM_CHECKS = 25
	}
}
