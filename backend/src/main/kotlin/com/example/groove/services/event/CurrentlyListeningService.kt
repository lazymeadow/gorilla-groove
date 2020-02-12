package com.example.groove.services.event

import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.services.DeviceService
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CurrentlyListeningService(
		val deviceService: DeviceService,
		val trackRepository: TrackRepository
) : EventService {
	private val currentlyListeningUsers = ConcurrentHashMap<Long, SongListen>()
	private var updateCount = -1

	override fun handlesEvent(eventType: EventType): Boolean {
		return eventType == EventType.NOW_PLAYING
	}

	override fun sendEvent(event: EventRequest) {
		event as NowPlayingEventRequest

		val user = loadLoggedInUser()

		val device = deviceService.getCurrentUsersDevice(event.deviceId)
		val track = event.trackId?.let { trackRepository.findById(it) }?.unwrap()

		val newTime = now().time
		synchronized(this) {
			if (track == null) {
				currentlyListeningUsers.remove(user.id)
				incrementUpdate()
				return
			}

			val currentListen = currentlyListeningUsers[user.id]
			val newListen = SongListen(
					song = track.getDisplayString(),
					deviceName = device.deviceName,
					isPhone = device.deviceType.isPhone,
					lastUpdate = newTime
			)

			if (newListen.song != currentListen?.song) {
				incrementUpdate()
			}

			currentlyListeningUsers[user.id] = newListen
		}
	}

	private fun Track.getDisplayString(): String {
		return if (private) {
			"This track is private"
		} else {
			"$artist - $name"
		}
	}

	override fun getEvent(deviceId: String?, lastUpdateId: Int): EventResponse? {
		if (lastUpdateId == updateCount) {
			return null
		}

		val user = loadLoggedInUser()

		val listeningUsers = currentlyListeningUsers
				.mapValues { SongListenResponse(
						song = it.value.song,
						deviceName = it.value.deviceName,
						isPhone = it.value.isPhone
				) }
				.filter { (userId, _) -> userId != user.id }

		return NowPlayingEventResponse(
				currentlyListeningUsers = listeningUsers,
				lastUpdateId = updateCount
		)
	}

	@Scheduled(fixedRate = 10000, initialDelay = 30000)
	fun cleanupOldListens() {
		val deleteOlderThan = now().time - 35 * 1000 // We should be updating every 15 seconds, but build in a buffer
		synchronized(this) {
			val listens = currentlyListeningUsers.toList()
			listens.forEach { (userId, songListen) ->
				if (songListen.lastUpdate < deleteOlderThan) {
					currentlyListeningUsers.remove(userId)
					incrementUpdate()
				}
			}
		}
	}

	private fun incrementUpdate() {
		updateCount++
		updateCount %= Int.MAX_VALUE
	}

	data class SongListen(
			val song: String,
			val deviceName: String?,
			val isPhone: Boolean?,
			val lastUpdate: Long // millis
	)
}
