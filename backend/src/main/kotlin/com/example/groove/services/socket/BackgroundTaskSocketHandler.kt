package com.example.groove.services.socket

import com.example.groove.db.model.BackgroundTaskItem
import com.example.groove.db.model.enums.DeviceType
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class BackgroundTaskSocketHandler(
		@Lazy private val socket: WebSocket
) {
	fun broadcastBackgroundTaskStatus(task: BackgroundTaskItem, trackId: Long? = null) {
		val sessions = socket.sessionsFor(userId = task.user.id, deviceType = DeviceType.WEB)

		val message = BackgroundTaskResponse(
				task = task,
				trackId = trackId
		)

		sessions.forEach { it.sendIfOpen(message) }
	}
}

data class BackgroundTaskResponse(
		override val messageType: EventType = EventType.BACKGROUND_TASK,
		val task: BackgroundTaskItem,
		val trackId: Long?
) : WebSocketMessage
