package com.example.groove.config

import com.example.groove.db.dao.TrackLinkRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.Track
import com.example.groove.security.SecurityConfiguration
import com.example.groove.services.ArtSize
import com.example.groove.services.DeviceService
import com.example.groove.services.event.EventType
import com.example.groove.services.event.NowPlayingTrack
import com.example.groove.util.createMapper
import com.example.groove.util.get
import com.example.groove.util.logger
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

@Configuration
@EnableWebSocket
class WebSocketConfig(
		private val userRepository: UserRepository,
		private val trackLinkRepository: TrackLinkRepository,
		private val trackRepository: TrackRepository,
		private val deviceService: DeviceService
) : WebSocketConfigurer {

	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry
				.addHandler(SocketTextHandler(trackLinkRepository, trackRepository, deviceService), "/api/socket")
				.setAllowedOrigins(*SecurityConfiguration.allowedOrigins)
				.setHandshakeHandler(object : DefaultHandshakeHandler() {
					override fun determineUser(request: ServerHttpRequest, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Principal? {
						// I know it's confusing that "name" is "email", that's because the email is unique, so that's what
						// is assigned to the name in the principal
						val email = request.principal!!.name

						// We can now look up this user based off the unique email in order to get the ID associated with them
						val user = userRepository.findByEmail(email)
								?: throw IllegalStateException("No user found with the email $email!")

						// Now throw it in the extra attributes so we can find it more readily as this user uses this session
						attributes["userId"] = user.id

						return request.principal
					}
				})
	}
}

@Component
class SocketTextHandler(
		private val trackLinkRepository: TrackLinkRepository,
		private val trackRepository: TrackRepository,
		private val deviceService: DeviceService
) : TextWebSocketHandler() {

	val objectMapper = createMapper()
	val sessions = mutableMapOf<String, WebSocketSession>()
	val currentSongListens = ConcurrentHashMap<String, NowListeningResponse>()

	override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
		val clientMessage = try {
			objectMapper.readValue(message.payload, WebSocketMessage::class.java)
		} catch (e: Exception) {
			logger.error("Could not deserialize WebSocket message! Message: $message", e)
			return
		}

		when (clientMessage) {
			is NowListeningRequest -> handleNowListeningData(session, clientMessage)
			else -> throw IllegalArgumentException("Incorrect message type!")
		}
	}

	private fun handleNowListeningData(session: WebSocketSession, nowListeningRequest: NowListeningRequest) {
		val nowListeningResponse = nowListeningRequest.toResponse(session)

		// currentSongListens holds onto the existing state of the device. Augment it with the new state as the new state
		// might not hold every property (the clients don't send every property every time any changes)
		val broadcastMessage = nowListeningResponse.merge(currentSongListens[session.id])
		currentSongListens[session.id] = broadcastMessage

		val otherSessions = sessions - session.id
		otherSessions.values.forEach { it.sendIfOpen(objectMapper.writeValueAsString(broadcastMessage)) }
	}

	override fun afterConnectionEstablished(session: WebSocketSession) {
		logger.info("New user connected to socket with ID: ${session.id}")
		sessions[session.id] = session

		// Tell this new user about all the things being listened to
		val timeForStaleListen = System.currentTimeMillis() - 30_000
		currentSongListens.values
				.filter { it.time > timeForStaleListen }
				.map { objectMapper.writeValueAsString(it) }
				.forEach { session.sendIfOpen(it) }
	}

	override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
		logger.info("User disconnected from socket with ID: ${session.id}")
		val lastSentUpdate = currentSongListens[session.id]

		sessions.remove(session.id)
		currentSongListens.remove(session.id)

		// If this user was not playing something, then there is no need to update any clients
		if (lastSentUpdate?.trackData == null) {
			return
		}

		val newUpdate = lastSentUpdate.copy(trackData = null)
		val message = objectMapper.writeValueAsString(newUpdate)
		sessions.values.forEach { it.sendIfOpen(message) }
	}

	fun WebSocketSession.sendIfOpen(message: String) {
		sendIfOpen(TextMessage(message))
	}

	fun WebSocketSession.sendIfOpen(message: TextMessage) {
		logger.info("About to broadcast '${message.payload}'")
		if (isOpen) {
			sendMessage(message)
		} else {
			logger.info("Could not send message to socket ID: $id: $message")
		}
	}

	private fun NowListeningRequest.toResponse(session: WebSocketSession): NowListeningResponse {
		val userId = session.attributes["userId"] as Long
		return NowListeningResponse(
				deviceId = this.deviceId,
				deviceName = deviceService.getDeviceByIdAndUserId(this.deviceId, userId).deviceName,
				userId = userId,
				timePlayed = this.timePlayed,
				isPlaying = this.isPlaying,
				isRepeating = this.isRepeating,
				isShuffling = this.isShuffling,
				volume = this.volume,
				muted = this.muted,
				trackData = this.trackId?.let { trackRepository.get(it) }?.toListenTrack()
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

	companion object {
		val logger = logger()
	}
}

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "messageType",
		visible = true
)
@JsonSubTypes(
	JsonSubTypes.Type(value = NowListeningRequest::class, name = "NOW_PLAYING_UPDATE")
)
interface WebSocketMessage {
		val messageType: EventType
}

data class NowListeningRequest(
		override val messageType: EventType,
		val deviceId: String,
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
		val deviceId: String,
		val deviceName: String,
		val userId: Long,
		val timePlayed: Double?,
		val trackData: NowPlayingTrack?,
		val isShuffling: Boolean?,
		val isRepeating: Boolean?,
		val isPlaying: Boolean?,
		val volume: Double?,
		val muted: Boolean?,
		val time: Long = System.currentTimeMillis()
) : WebSocketMessage


inline fun <reified T : Any> T.merge(other: T?): T {
	if (other == null) {
		return this
	}

	val propertiesByName = T::class.declaredMemberProperties.associateBy { it.name }
	val primaryConstructor = T::class.primaryConstructor
			?: throw IllegalArgumentException("merge type must have a primary constructor")
	val args = primaryConstructor.parameters.associateWith { parameter ->
		val property = propertiesByName[parameter.name]
				?: throw IllegalStateException("no declared member property found with name '${parameter.name}'")
		(property.get(this) ?: property.get(other))
	}
	return primaryConstructor.callBy(args)
}
