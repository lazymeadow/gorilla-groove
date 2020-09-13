package com.example.groove.services.socket

import com.example.groove.db.model.enums.DeviceType
import com.example.groove.util.createMapper
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

private val objectMapper = createMapper()

fun WebSocketSession.sendIfOpen(message: Any) {
	val textMessage = TextMessage(objectMapper.writeValueAsString(message))

	WebSocket.logger.debug("About to broadcast to user: $userId, session: $id, deviceIdentifier: $deviceIdentifier, '${textMessage.payload}'")
	// If two threads access the same session it's an error. "The remote endpoint was in state [TEXT_PARTIAL_WRITING]"
	synchronized(this) {
		if (isOpen) {
			sendMessage(textMessage)
		} else {
			WebSocket.logger.warn("Could not send message to user: $userId, socket ID: $id: $message")
		}
	}
}

var WebSocketSession.userId: Long
	get() = this.attributes["userId"] as Long
	set(value) {
		this.attributes["userId"] = value
	}

var WebSocketSession.deviceIdentifier: String
	get() = this.attributes["deviceIdentifier"] as String
	set(value) {
		this.attributes["deviceIdentifier"] = value
	}

var WebSocketSession.deviceType: DeviceType
	get() = this.attributes["deviceType"] as DeviceType
	set(value) {
		this.attributes["deviceType"] = value
	}
