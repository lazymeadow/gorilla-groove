package com.example.groove.services.event

import com.example.groove.db.model.Device
import com.example.groove.services.TrackService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceEventService(
		private val trackService: TrackService
) : EventService {

	private val deviceEvents = ConcurrentHashMap<Long, MutableList<EventResponse>>()

	override fun handlesEvent(eventType: EventType): Boolean {
		return eventType == EventType.REMOTE_PLAY
	}

	override fun getEvent(sourceDevice: Device, lastEventId: Int): EventResponse? {
		// Could easily be a striped lock on the deviceID when we have more concurrent users and need to
		synchronized(this) {
			return deviceEvents[sourceDevice.id]?.removeAtOrNull(0)
		}
	}

	private fun<T> MutableList<T>.removeAtOrNull(index: Int): T? {
		if (isEmpty()) {
			return null
		}
		return this.removeAt(index)
	}

	// I started making this delegate try to work for any Device-based event, but right now it only works
	// with remote play. Will need a bit of another pass to get it to work with more
	override fun sendEvent(sourceDevice: Device, event: EventRequest) {
		event as RemotePlayEventRequest

		val trackIdToTrack = trackService
				.getTracksByIds(event.trackIds?.toSet() ?: emptySet())
				.map { it.id to it }
				.toMap()

		// A user could, theoretically, tell us to play a single track ID more than once.
		// So load all the unique tracks belonging to the IDs from the DB, and then iterate
		// over the IDs we are given so we preserve any duplicate IDs that we were given
		val tracksToPlay = event.trackIds?.map { trackIdToTrack.getValue(it) }

		val eventResponse = RemotePlayEventResponse(
				tracks = tracksToPlay,
				remotePlayAction = event.remotePlayAction
		)

		val targetDeviceId = event.targetDeviceId
		synchronized(this) {
			if (deviceEvents[targetDeviceId] == null) {
				deviceEvents[targetDeviceId] = mutableListOf<EventResponse>(eventResponse)
			} else {
				deviceEvents.getValue(targetDeviceId).add(eventResponse)
			}
		}
	}
}
