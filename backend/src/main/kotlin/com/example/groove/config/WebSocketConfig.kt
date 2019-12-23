package com.example.groove.config

import com.example.groove.security.SecurityConfiguration
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry
				.addHandler(SocketTextHandler(), "/api/socket")
				.setAllowedOrigins(*SecurityConfiguration.allowedOrigins)
	}
}

// FIXME this socket really doesn't belong here, and it is also super specific to one use case
// It should be moved out, and be made more generic and look at message types for the future
@Component
class SocketTextHandler : TextWebSocketHandler() {

	val objectMapper = jacksonObjectMapper()
	val sessions = mutableSetOf<WebSocketSession>()
	val currentSongListens = ConcurrentHashMap<String, NowListeningDTO>()

	override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
		val nowListeningDTO = try {
			objectMapper.readValue(message.payload, NowListeningDTO::class.java)
		} catch (e: Exception) {
			logger.error("Could not deserialize WebSocket message! Message: $message", e)
			return
		}

		currentSongListens[session.id] = nowListeningDTO

		val otherSessions = sessions - session
		otherSessions.forEach { it.sendIfOpen(message) }
	}

	override fun afterConnectionEstablished(session: WebSocketSession) {
		logger.debug("New user connected to socket with ID: ${session.id}")
		sessions.add(session)

		// Tell this new user about all the things being listened to
		val timeForStaleListen = System.currentTimeMillis() - 30_000
		currentSongListens.values
				.filter { it.time > timeForStaleListen }
				.map { objectMapper.writeValueAsString(it) }
				.forEach { session.sendIfOpen(it) }
	}

	override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
		logger.debug("User disconnected from socket with ID: ${session.id}")
		val lastSentUpdate = currentSongListens[session.id]

		sessions.remove(session)
		currentSongListens.remove(session.id)

		// If this user was not playing something, then there is no need to update any clients
		if (lastSentUpdate?.trackId == null) {
			return
		}

		val newUpdate = lastSentUpdate.copy(trackId = null)
		val message = objectMapper.writeValueAsString(newUpdate)
		sessions.forEach { it.sendIfOpen(message) }
	}

	fun WebSocketSession.sendIfOpen(message: String) {
		sendIfOpen(TextMessage(message))
	}

	fun WebSocketSession.sendIfOpen(message: TextMessage) {
		if (isOpen) {
			sendMessage(message)
		} else {
			logger.info("Could not send message to socket ID: $id: $message")
		}
	}

	companion object {
		val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)!!
	}
}

data class NowListeningDTO(
	val userEmail: String,
	val trackId: Long?,
	val trackName: String?,
	val trackArtist: String?,
	val time: Long = System.currentTimeMillis()
)
