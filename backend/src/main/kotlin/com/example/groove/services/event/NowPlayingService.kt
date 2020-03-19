package com.example.groove.services.event

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Device
import com.example.groove.db.model.Track
import com.example.groove.services.DeviceService
import com.example.groove.util.DateUtils.now
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.unwrap
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class NowPlayingService(
		val deviceService: DeviceService,
		val trackRepository: TrackRepository,
		val trackLinkRepository: TrackLinkRepository
) : EventService {

	// User ID -> all devices that user is listening to songs on
	private val currentlyListeningUsers = ConcurrentHashMap<Long, MutableList<SongListenResponse>>()
	private var updateCount = -1

	override fun handlesEvent(eventType: EventType): Boolean {
		return eventType == EventType.NOW_PLAYING
	}

	override fun sendEvent(sourceDevice: Device, event: EventRequest) {
		event as NowPlayingEventRequest

		val user = loadLoggedInUser()

		val device = deviceService.getDeviceById(event.deviceId)
		val track = event.trackId?.let { trackRepository.findById(it) }?.unwrap()

		val newTime = now().time

		// This makes a DB call, so it's important to do it outside the lock to minimize time spent locked
		val trackData = track?.asListenTrack()

		synchronized(this) {
			if (currentlyListeningUsers[user.id] == null) {
				currentlyListeningUsers[user.id] = mutableListOf()
			}

			// User specifically disconnected this device. Remove all information
			if (event.disconnected) {
				val nowPlayingData = currentlyListeningUsers.getValue(user.id).find { it.deviceId == device.id }
				currentlyListeningUsers.getValue(user.id).remove(nowPlayingData)

				incrementUpdate()
				return
			}

			val currentListen = currentlyListeningUsers[user.id]?.find { it.deviceId == device.id }
			currentlyListeningUsers[user.id]?.remove(currentListen)

			val newListen = currentListen?.copy(
					trackData = if (event.removeTrack) { null } else { trackData ?: currentListen.trackData },
					deviceName = device.deviceName,
					lastUpdate = newTime,
					lastTimeUpdate = if (event.timePlayed != null) { newTime } else { currentListen.lastTimeUpdate },
					volume = event.volume ?: currentListen.volume,
					muted = event.muted ?: currentListen.muted,
					timePlayed = if (event.removeTrack) { 0.0 } else { event.timePlayed ?: currentListen.timePlayed },
					isShuffling = event.isShuffling ?: currentListen.isShuffling,
					isRepeating = event.isRepeating ?: currentListen.isRepeating,
					isPlaying = if (event.removeTrack) { false } else { event.isPlaying ?: currentListen.isPlaying }
			) ?: SongListenResponse(
					trackData = trackData,
					deviceId = device.id,
					deviceName = device.deviceName,
					isPhone = device.deviceType.isPhone,
					lastUpdate = newTime,
					lastTimeUpdate = newTime,
					volume = event.volume ?: 0.0,
					muted = event.muted ?: false,
					timePlayed = event.timePlayed ?: 0.0,
					isShuffling = event.isShuffling ?: false,
					isRepeating = event.isRepeating ?: false,
					isPlaying = event.isPlaying ?: false
			)

			incrementUpdate()

			currentlyListeningUsers.getValue(user.id).add(newListen)
		}
	}

	// Get a trimmed down version (that also includes an art link)
	private fun Track.asListenTrack(): SongListenTrack? {
		return when {
			private -> SongListenTrack(isPrivate = true)
			else -> SongListenTrack(
					title = name,
					name = name,
					artist = artist,
					album = album,
					releaseYear = releaseYear,
					duration = length,
					length = length,
					isPrivate = false,
					albumArtLink = trackLinkRepository.findUnexpiredArtByTrackId(id)?.link
			)
		}
	}

	override fun getEvent(sourceDevice: Device, lastEventId: Int): EventResponse? {
		if (lastEventId == updateCount) {
			return null
		}

		val listeningUsers = synchronized(this) {
			// Remove our own device as there's no reason to send it
			currentlyListeningUsers
					.map { (userId, devicesForUser) ->
						userId to devicesForUser.filter { it.deviceId != sourceDevice.id }
					}.toMap()
		}

		return NowPlayingEventResponse(
				currentlyListeningUsers = listeningUsers,
				lastEventId = updateCount
		)
	}

	@Scheduled(fixedRate = 10000, initialDelay = 30000)
	fun cleanupOldListens() {
		val deleteOlderThan = now().time - 45 * 1000 // We should be updating every 20 seconds, but build in a buffer
		synchronized(this) {
			val userDevices = currentlyListeningUsers.toList()
			userDevices.forEach { (_, songResponses) ->
				songResponses.forEach { songResponse ->
					if (songResponse.lastUpdate < deleteOlderThan) {
						// If we haven't seen an update in a while, it's probably safe to assume it stopped playing
						songResponse.isPlaying = false
						incrementUpdate()
					}
				}
			}
		}
	}

	private fun incrementUpdate() {
		updateCount++
		updateCount %= Int.MAX_VALUE
	}
}
