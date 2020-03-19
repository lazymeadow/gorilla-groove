package com.example.groove.services.event

import com.example.groove.db.dao.DeviceRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.Device
import com.example.groove.services.DeviceService
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap


@Service
class EventServiceCoordinator(
		private val eventServices: List<EventService>,
		private val deviceService: DeviceService,
		private val deviceRepository: DeviceRepository,
		private val userRepository: UserRepository
) {

	// User ID -> (Device.id -> Last Seen Time)
	private val connectedDevices = ConcurrentHashMap<Long, MutableMap<Long, Long>>()

	fun getEvent(deviceId: String, lastEventId: Int): EventResponse? {
		val user = loadLoggedInUser()
		val device = deviceService.getDeviceById(deviceId)

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

		val mergedDevice = deviceService.getDeviceById(event.deviceId)

		service.sendEvent(mergedDevice, event)
	}

	@Transactional(readOnly = true)
	fun getActiveDevices(excludingDeviceId: String?): List<Device> {
		// Load the user in this transaction so we can load its partyDevices.
		// If we try to access the devices otherwise we'll get a LazyInitializationException
		val user = userRepository.findById(loadLoggedInUser().id).unwrap()!!

		// Grab all of our own active devices (including our current one) and then add in all
		// devices we have access to via Party Mode
		val currentDevice = excludingDeviceId?.let { deviceService.getDeviceById(it) }

		val ownDevices = if (connectedDevices.containsKey(user.id)) {
			connectedDevices.getValue(user.id)
					.filterNot { (deviceId, _) -> deviceId == currentDevice?.id }
					.map { deviceRepository.findById(it.key).unwrap()!! }
		} else {
			emptyList()
		}

		val now = now()

		val partyDevices = user.partyDevices.filter {
			it.partyEnabledUntil != null && it.partyEnabledUntil!! > now
		}

		val devices = ownDevices + partyDevices

		// Finally, remove devices from our list if they have not been seen polling
		return devices.filter { device ->
			val lastSeen = connectedDevices[device.user.id]?.get(device.id) ?: 0

			// A device that's connected should be continually polling for new things.
			// So the most time that should realistically elapse between two checks (and thus
			// the next "lastSeen" is the interval * the number of checks.
			// But build in a small buffer of a few seconds in case there is a hiccup
			val expiredTime = (CHECK_INTERVAL * NUM_CHECKS) + 4000L
			lastSeen > now.time - expiredTime
		}
	}

	companion object {
		private const val CHECK_INTERVAL = 1000L
		private const val NUM_CHECKS = 55
	}
}
