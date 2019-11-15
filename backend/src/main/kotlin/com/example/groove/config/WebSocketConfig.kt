package com.example.groove.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
				.setAllowedOrigins("*")
	}

}

@Component
class SocketTextHandler : TextWebSocketHandler() {

	val objectMapper = jacksonObjectMapper()
	val sessions = mutableSetOf<WebSocketSession>()
	val currentSongListens = ConcurrentHashMap<String, NowListeningDTO>()

	override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
		val nowListeningDTO = objectMapper.readValue(message.payload, NowListeningDTO::class.java)

		currentSongListens[session.id] = nowListeningDTO

		val otherSessions = sessions - session
		otherSessions.forEach { it.sendMessage(message) }
	}

	override fun afterConnectionEstablished(session: WebSocketSession) {
		sessions.add(session)

		// Tell this new user about all the things being listened to
		val timeForStaleListen = System.currentTimeMillis() - 30_000
		currentSongListens.values
				.filter { it.time > timeForStaleListen }
				.map { objectMapper.writeValueAsString(it) }
				.forEach { session.sendMessage(TextMessage(it)) }
	}

	override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
		val lastSentUpdate = currentSongListens[session.id]

		sessions.remove(session)
		currentSongListens.remove(session.id)

		// If this user was playing something, broadcast to everyone that they stopped
		if (lastSentUpdate?.trackId != null) {
			val newUpdate = lastSentUpdate.copy(trackId = null)
			val message = objectMapper.writeValueAsString(newUpdate)
			sessions.forEach { it.sendMessage(TextMessage(message)) }
		}
	}
}

data class NowListeningDTO(
	val userEmail: String,
	val trackId: Long?,
	val trackName: String?,
	val trackArtist: String?,
	val time: Long = System.currentTimeMillis()
)
