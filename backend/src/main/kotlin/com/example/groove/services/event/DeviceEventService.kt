package com.example.groove.services.event

import com.example.groove.db.model.Device
import com.example.groove.exception.PermissionDeniedException
import com.example.groove.services.DeviceService
import com.example.groove.services.TrackService
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceEventService(
		private val trackService: TrackService,
		private val deviceService: DeviceService
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

		val user = loadLoggedInUser()
		val targetDeviceId = event.targetDeviceId

		val targetDevice = deviceService.getDeviceById(targetDeviceId)

		if (!targetDevice.canBePlayedBy(loadLoggedInUser().id)) {
			throw PermissionDeniedException("Not authorized to access device")
		}

		val trackIdToTrack = trackService
				.getTracksByIds(event.trackIds?.toSet() ?: emptySet())
				// Don't allow playing your own private songs to someone who isn't you. It won't load for them anyway
				.filter { !it.private || targetDevice.user.id == user.id }
				.map { it.id to it }
				.toMap()

		// A user could, theoretically, tell us to play a single track ID more than once.
		// So load all the unique tracks belonging to the IDs from the DB, and then iterate
		// over the IDs we are given so we preserve any duplicate IDs
		val tracksToPlay = event.trackIds?.map { trackIdToTrack.getValue(it) }

		val eventResponse = RemotePlayEventResponse(
				tracks = tracksToPlay,
				newFloatValue = event.newFloatValue,
				remotePlayAction = event.remotePlayAction
		)

		synchronized(this) {
			if (deviceEvents[targetDeviceId] == null) {
				deviceEvents[targetDeviceId] = mutableListOf<EventResponse>(eventResponse)
			} else {
				deviceEvents.getValue(targetDeviceId).add(eventResponse)
			}
		}
	}

	private fun Device.canBePlayedBy(userId: Long): Boolean {
		// If we are the user, then we're always good
		if (userId == user.id) {
			return true
		}

		// Check if we're in a valid party mode. If we aren't, then this isn't playable by other people
		if (partyEnabledUntil == null || partyEnabledUntil!! < now()) {
			return false
		}

		// We're in a valid party mode. Make sure the user who is controlling us is present in the list
		return partyUsers.any { it.id == userId }
	}
}
