package com.example.groove.services.event

import com.fasterxml.jackson.annotation.JsonProperty

interface EventRequest {
	val deviceId: String
}
interface EventResponse {
	var lastUpdateId: Int
}

interface EventService {
	fun handlesEvent(eventType: EventType): Boolean
	fun getEvent(deviceId: String?, lastUpdateId: Int): EventResponse?
	fun sendEvent(event: EventRequest)
}

enum class EventType {
	NOW_PLAYING, REMOTE_PLAY
}

data class NowPlayingEventRequest(
		val trackId: Long?,
		override val deviceId: String
) : EventRequest

data class RemotePlayEventRequest(
		val targetDeviceId: String,
		val trackIds: List<Long>,
		override val deviceId: String
) : EventRequest

data class RemotePlayEventResponse(
		val trackIds: List<Long>,
		override var lastUpdateId: Int = -1
) : EventResponse

data class NowPlayingEventResponse(
		val currentlyListeningUsers: Map<Long, SongListenResponse>?,
		override var lastUpdateId: Int
) : EventResponse

data class SongListenResponse(
		val song: String,
		val deviceName: String?,

		// Damn Jackson stripping out the "is". No, I want it there
		@get:JsonProperty("isPhone")
		val isPhone: Boolean?
)
