package com.example.groove.services.event

import com.example.groove.db.model.Device
import com.example.groove.db.model.Track
import com.fasterxml.jackson.annotation.JsonProperty

interface EventRequest {
	val deviceId: String
}
interface EventResponse {
	var lastEventId: Int
	val eventType: EventType
}

interface EventService {
	fun handlesEvent(eventType: EventType): Boolean
	fun getEvent(sourceDevice: Device, lastEventId: Int): EventResponse?
	fun sendEvent(sourceDevice: Device, event: EventRequest)
}

enum class EventType {
	NOW_PLAYING, REMOTE_PLAY
}

enum class RemotePlayType {
	PLAY_SET_SONGS, ADD_SONGS_NEXT, ADD_SONGS_LAST, PAUSE, PLAY, SEEK
}

data class NowPlayingEventRequest(
		val trackId: Long?,
		override val deviceId: String
) : EventRequest

data class RemotePlayEventRequest(
		val targetDeviceId: Long,
		val trackIds: List<Long>?,
		val remotePlayAction: RemotePlayType,
		override val deviceId: String
) : EventRequest

data class RemotePlayEventResponse(
		val tracks: List<Track>?,
		val remotePlayAction: RemotePlayType,
		override var lastEventId: Int = -1,
		override val eventType: EventType = EventType.REMOTE_PLAY
) : EventResponse

data class NowPlayingEventResponse(
		val currentlyListeningUsers: Map<Long, SongListenResponse>?,
		override var lastEventId: Int,
		override val eventType: EventType = EventType.NOW_PLAYING
) : EventResponse

data class SongListenResponse(
		val song: String,
		val deviceName: String?,

		// Damn Jackson stripping out the "is". No, I want it there
		@get:JsonProperty("isPhone")
		val isPhone: Boolean?
)
