package com.example.groove.services.socket

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession


fun WebSocketSession.sendIfOpen(message: String) {
	sendIfOpen(TextMessage(message))
}

fun WebSocketSession.sendIfOpen(message: TextMessage) {
	WebSocket.logger.info("About to broadcast to user: $userId, session $id: '${message.payload}'")
	if (isOpen) {
		sendMessage(message)
	} else {
		WebSocket.logger.info("Could not send message to user: $userId, socket ID: $id: $message")
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
