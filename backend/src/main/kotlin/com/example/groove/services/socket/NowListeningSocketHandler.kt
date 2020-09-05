package com.example.groove.services.socket

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.Track
import com.example.groove.services.ArtSize
import com.example.groove.services.DeviceService
import com.example.groove.services.event.EventType
import com.example.groove.services.event.NowPlayingTrack
import com.example.groove.util.createMapper
import com.example.groove.util.get
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class NowListeningSocketHandler(
		@Lazy private val socket: WebSocket,
		private val deviceService: DeviceService,
		private val trackRepository: TrackRepository,
		private val trackLinkRepository: TrackLinkRepository
) : SocketHandler<NowListeningRequest> {

	private val objectMapper = createMapper()
	private val currentSongListens = ConcurrentHashMap<String, NowListeningResponse>()

	override fun handleMessage(session: WebSocketSession, data: NowListeningRequest) {
		val nowListeningResponse = data.toResponse(session)

		// currentSongListens holds onto the existing state of the device. Augment it with the new state as the new state
		// might not hold every property (the clients don't send every property every time any changes)
		val broadcastMessage = nowListeningResponse.merge(currentSongListens[session.id])
		currentSongListens[session.id] = broadcastMessage

		val otherSessions = socket.sessions - session.id
		otherSessions.values.forEach { it.sendIfOpen(objectMapper.writeValueAsString(broadcastMessage)) }
	}

	fun sendAllListensToSession(session: WebSocketSession) {
		currentSongListens.values
				.map { objectMapper.writeValueAsString(it) }
				.forEach { session.sendIfOpen(it) }
	}

	fun removeSession(session: WebSocketSession) {
		val lastSentUpdate = currentSongListens[session.id]
		currentSongListens.remove(session.id)

		// If this user was not playing something, then there is no need to update any clients
		if (lastSentUpdate?.trackData == null) {
			return
		}

		val newUpdate = lastSentUpdate.copy(trackData = null)
		val message = objectMapper.writeValueAsString(newUpdate)
		socket.sessions.values.forEach { it.sendIfOpen(message) }
	}

	private fun NowListeningRequest.toResponse(session: WebSocketSession): NowListeningResponse {
		val userId = session.userId
		val device = deviceService.getDeviceByIdAndUserId(session.deviceIdentifier, userId)
		return NowListeningResponse(
				deviceId = device.id,
				deviceName = device.deviceName,
				userId = userId,
				timePlayed = this.timePlayed,
				isPlaying = this.isPlaying,
				isRepeating = this.isRepeating,
				isShuffling = this.isShuffling,
				volume = this.volume,
				muted = this.muted,
				trackData = this.trackId?.let { trackRepository.get(it) }?.toListenTrack(),
				lastTimeUpdate = if (this.timePlayed != null) {
					System.currentTimeMillis()
				} else {
					null
				}
		)
	}

	// Get a trimmed down version (that also includes an art link)
	private fun Track.toListenTrack(): NowPlayingTrack? {
		return when {
			private -> NowPlayingTrack(isPrivate = true)
			else -> NowPlayingTrack(
					id = id,
					name = name,
					artist = artist,
					album = album,
					releaseYear = releaseYear,
					length = length,
					isPrivate = false,
					inReview = inReview,
					albumArtLink = trackLinkRepository.findUnexpiredArtByTrackIdAndArtSize(id, ArtSize.LARGE)?.link
			)
		}
	}
}

data class NowListeningRequest(
		override val messageType: EventType,
		val timePlayed: Double?,
		val trackId: Long?,
		val isShuffling: Boolean?,
		val isRepeating: Boolean?,
		val isPlaying: Boolean?,
		val volume: Double?,
		val muted: Boolean?
) : WebSocketMessage

data class NowListeningResponse(
		override val messageType: EventType = EventType.NOW_PLAYING,
		val deviceId: Long,
		val deviceName: String,
		val userId: Long,
		val timePlayed: Double?,
		val trackData: NowPlayingTrack?,
		val isShuffling: Boolean?,
		val isRepeating: Boolean?,
		val isPlaying: Boolean?,
		val volume: Double?,
		val muted: Boolean?,
		val lastTimeUpdate: Long? // millis
) : WebSocketMessage
