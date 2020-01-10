package com.example.groove.services

import com.example.groove.db.model.User
import com.example.groove.dto.CurrentlyListeningUsersDTO
import com.example.groove.util.DateUtils.now
import com.example.groove.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CurrentlyListeningService {
	private val currentlyListeningUsers = ConcurrentHashMap<Long, SongListen>()
	private var updateCount = 0

	fun setListeningUser(user: User, song: String) {
		val newTime = now().time
		synchronized(this) {
			val currentListen = currentlyListeningUsers[user.id]
			val newListen = currentListen?.apply { lastUpdate = newTime } ?: SongListen(song, newTime)

			if (newListen.song != currentListen?.song) {
				updateCount++
				updateCount %= Int.MAX_VALUE
			}

			currentlyListeningUsers[user.id] = newListen
		}
	}

	fun getListeningUsersIfNew(currentUser: User, lastUpdateCount: Int): CurrentlyListeningUsersDTO? {
		return if (lastUpdateCount != updateCount) {
			val listeningUsers = currentlyListeningUsers
					.map { (userId, listen) -> userId to listen.song }
					.filter { (userId, _) -> userId != currentUser.id }

			if (listeningUsers.isEmpty()) {
				return null
			}

			return CurrentlyListeningUsersDTO(currentlyListeningUsers = listeningUsers, lastUpdate = updateCount)
		} else {
			null
		}
	}

	@Scheduled(fixedRate = 1000)
	private fun cleanupOldListens() {
		logger.info("Cleaning up old listens...")
		val deleteOlderThan = now().time - 35 * 1000 // We should be updating every 15 seconds, but build in a buffer
		synchronized(this) {
			val listens = currentlyListeningUsers.toList()
			listens.forEach { (userId, songListen) ->
				if (songListen.lastUpdate < deleteOlderThan) {
					logger.info("Deleting listen for user $userId")
					currentlyListeningUsers.remove(userId)
				}
			}
		}
	}

	data class SongListen(
		val song: String,
		var lastUpdate: Long // millis
	)

	companion object {
		private val logger = logger<CurrentlyListeningService>()
	}
}
