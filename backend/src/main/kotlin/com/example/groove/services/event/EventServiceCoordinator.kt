package com.example.groove.services.event

import com.example.groove.db.dao.DeviceRepository
import com.example.groove.db.model.Device
import com.example.groove.services.DeviceService
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap


@Service
class EventServiceCoordinator(
		private val eventServices: List<EventService>,
		private val deviceService: DeviceService,
		private val deviceRepository: DeviceRepository
) {

	// User ID -> (Device.id -> Last Seen Time)
	private val connectedDevices = ConcurrentHashMap<Long, MutableMap<Long, Long>>()

	fun getEvent(deviceId: String, lastEventId: Int): EventResponse? {
		val user = loadLoggedInUser()
		val device = deviceService.getCurrentUsersDevice(deviceId)

		synchronized(this) {
			if (!connectedDevices.containsKey(user.id)) {
				connectedDevices[user.id] = mutableMapOf()
			}

			connectedDevices.getValue(user.id)[device.id] = now().time
		}

		// Long poll for updates to events.
		// The first time we see a new event we will return
		for (i in 0..NUM_CHECKS) {
			eventServices.forEach { eventServices ->
				eventServices.getEvent(device, lastEventId)?.let {
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

		val mergedDevice = deviceService.getCurrentUsersDevice(event.deviceId)

		service.sendEvent(mergedDevice, event)
	}

	fun getActiveDevices(excludingDeviceId: String?): Set<Device> {
		val user = loadLoggedInUser()

		if (!connectedDevices.containsKey(user.id)) {
			return emptySet()
		}

		val excludingDevice = excludingDeviceId?.let { deviceService.getCurrentUsersDevice(it) }

		return connectedDevices.getValue(user.id)
				.filter { (deviceId, lastSeen) ->
					if (excludingDevice?.id == deviceId) {
						return@filter false
					}

					// A device that's connected should be continually polling for new things.
					// So the most time that should realistically elapse between two checks (and thus
					// the next "lastSeen" is the interval * the number of checks.
					// But build in a small buffer of a few seconds in case there is a hiccup
					val expiredTime = (CHECK_INTERVAL * NUM_CHECKS) + 4000L
					lastSeen > now().time - expiredTime
				}
				.map { deviceRepository.findById(it.key).unwrap()!! }
				.toSet()
	}

	companion object {
		private const val CHECK_INTERVAL = 1500L
		private const val NUM_CHECKS = 34
	}
}
