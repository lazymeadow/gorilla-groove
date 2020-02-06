package com.example.groove.services

import com.example.groove.db.model.User
import com.example.groove.dto.CurrentlyListeningUsersDTO
import com.example.groove.dto.SongListenResponse
import com.example.groove.util.DateUtils.now
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CurrentlyListeningService(val deviceService: DeviceService) {
	private val currentlyListeningUsers = ConcurrentHashMap<Long, SongListen>()
	private var updateCount = -1

	fun setListeningUser(user: User, song: String?, deviceId: String?) {
		val device = deviceId?.let { deviceService.getDevice(it) }

		val newTime = now().time
		synchronized(this) {
			if (song == null) {
				currentlyListeningUsers.remove(user.id)
				incrementUpdate()
				return
			}

			val currentListen = currentlyListeningUsers[user.id]
			val newListen = SongListen(
					song = song,
					deviceName = device?.deviceName,
					isPhone = device?.deviceType?.isPhone,
					lastUpdate = newTime
			)

			if (newListen.song != currentListen?.song) {
				incrementUpdate()
			}

			currentlyListeningUsers[user.id] = newListen
		}
	}

	fun getListeningUsersIfNew(currentUser: User, lastUpdateCount: Int): CurrentlyListeningUsersDTO? {
		if (lastUpdateCount == updateCount) {
			return null
		}

		val listeningUsers = currentlyListeningUsers
				.mapValues { SongListenResponse(
						song = it.value.song,
						deviceName = it.value.deviceName,
						isPhone = it.value.isPhone
				) }
				.filter { (userId, _) -> userId != currentUser.id }

		return CurrentlyListeningUsersDTO(
				currentlyListeningUsers = listeningUsers,
				lastUpdate = updateCount
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
