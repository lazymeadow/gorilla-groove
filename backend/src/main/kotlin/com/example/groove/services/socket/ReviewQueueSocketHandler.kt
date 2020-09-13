package com.example.groove.services.socket

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.ReviewSource
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.db.model.enums.ReviewSourceType
import com.example.groove.util.get
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import kotlin.concurrent.thread

@Service
class ReviewQueueSocketHandler(
		@Lazy private val socket: WebSocket,
		private val userRepository: UserRepository
) {

	fun broadcastNewReviewQueueContent(userId: Long, reviewSource: ReviewSource, addCount: Int) {
		val user = userRepository.get(userId)!!
		val sessions = socket.sessionsFor(userId = user.id, deviceType = DeviceType.WEB)

		val message = ReviewQueueResponse(
				sourceDisplayName = reviewSource.displayName,
				sourceType = reviewSource.sourceType,
				count = addCount
		)

		// Definitely pretty stupid, but the various review queue things happen inside of DB transactions and
		// if we broadcast the event too soon the transaction won't have yet been committed because we call this
		// from within that transaction (it's just easier, code-wise to do so). So spawn a thread and wait a small
		// amount of time for the commits to have almost certainly been finalized and the frontend won't get stale data
		thread {
			Thread.sleep(1000)
			sessions.forEach { it.sendIfOpen(message) }
		}
	}
}

data class ReviewQueueResponse(
		override val messageType: EventType = EventType.REVIEW_QUEUE,
		val sourceType: ReviewSourceType,
		val sourceDisplayName: String,
		val count: Int
) : WebSocketMessage
