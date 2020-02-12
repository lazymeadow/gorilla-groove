package com.example.groove.services.event

import com.example.groove.services.DeviceService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceEventService(
		private val deviceService: DeviceService
) : EventService {

	private val deviceEvents = ConcurrentHashMap<String, MutableList<EventResponse>>()

	override fun handlesEvent(eventType: EventType): Boolean {
		return eventType == EventType.REMOTE_PLAY
	}

	override fun getEvent(deviceId: String?, lastUpdateId: Int): EventResponse? {
		if (deviceId == null) {
			return null
		}

		// This throws if the device isn't valid for the current user. Just a permissions check
		deviceService.getCurrentUsersDevice(deviceId)

		// Could easily be a striped lock on the deviceID when we have more people...
		synchronized(this) {
			return deviceEvents[deviceId]?.removeAt(0)
		}
	}

	override fun sendEvent(event: EventRequest) {
		event as RemotePlayEventRequest

		val deviceId = event.deviceId
		val eventResponse = RemotePlayEventResponse(trackIds = event.trackIds)

		synchronized(this) {
			if (deviceEvents[deviceId] == null) {
				deviceEvents[deviceId] = mutableListOf<EventResponse>(eventResponse)
			} else {
				deviceEvents.getValue(deviceId).add(eventResponse)
			}
		}
	}
}
