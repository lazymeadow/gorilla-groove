package com.example.groove.services.event

import com.example.groove.db.model.Device
import com.example.groove.db.model.Track
import com.fasterxml.jackson.annotation.JsonIgnore
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

@Suppress("unused")
enum class RemotePlayType {
	PLAY_SET_SONGS, PLAY_NEXT, PLAY_PREVIOUS, ADD_SONGS_NEXT, ADD_SONGS_LAST,
	PAUSE, PLAY, SEEK, SHUFFLE_ENABLE, SHUFFLE_DISABLE, REPEAT_ENABLE,
	REPEAT_DISABLE, SET_VOLUME, MUTE, UNMUTE
}

data class NowPlayingEventRequest(
		val trackId: Long?, // Jackson doesn't differentiate between a null key and a null value,
		val removeTrack: Boolean = false, // so have a separate property to explicitly remove the played track
		val disconnected: Boolean = false,
		val isShuffling: Boolean?,
		val isRepeating: Boolean?,
		val isPlaying: Boolean?,
		val timePlayed: Double?,
		val volume: Double?,
		val muted: Boolean?,
		override val deviceId: String
) : EventRequest

data class RemotePlayEventRequest(
		val targetDeviceId: Long,
		val trackIds: List<Long>?,
		val newFloatValue: Double?,
		val remotePlayAction: RemotePlayType,
		override val deviceId: String
) : EventRequest

data class RemotePlayEventResponse(
		val tracks: List<Track>?,
		val newFloatValue: Double?,
		val remotePlayAction: RemotePlayType,
		override var lastEventId: Int = -1,
		override val eventType: EventType = EventType.REMOTE_PLAY
) : EventResponse

data class NowPlayingEventResponse(
		val currentlyListeningUsers: Map<Long, List<SongListenResponse>>?,
		override var lastEventId: Int,
		override val eventType: EventType = EventType.NOW_PLAYING
) : EventResponse

data class SongListenResponse(
		val trackData: SongListenTrack?,
		val deviceName: String?,

		// Damn Jackson stripping out the "is". No, I want it there
		@get:JsonProperty("isPhone")
		val isPhone: Boolean?,

		val deviceId: Long,
		val volume: Double,
		val muted: Boolean,
		val timePlayed: Double, // In seconds
		val isShuffling: Boolean,
		val isRepeating: Boolean,
		var isPlaying: Boolean,
		val lastTimeUpdate: Long, // millis

		@JsonIgnore
		val lastUpdate: Long // millis
)

data class SongListenTrack(
		val id: Long? = null,

		val name: String? = null,
		val artist: String? = null,
		val album: String? = null,
		val releaseYear: Int? = null,
		val albumArtLink: String? = null,
		val length: Int? = null,
		val inReview: Boolean? = null,

		@get:JsonProperty("isPrivate")
		val isPrivate: Boolean = false
)
