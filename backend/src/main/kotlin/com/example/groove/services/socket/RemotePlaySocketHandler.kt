package com.example.groove.services.socket

import com.example.groove.db.dao.UserRepository
import com.example.groove.db.model.Device
import com.example.groove.db.model.Track
import com.example.groove.exception.PermissionDeniedException
import com.example.groove.services.DeviceService
import com.example.groove.services.TrackService
import com.example.groove.services.event.EventType
import com.example.groove.services.event.RemotePlayType
import com.example.groove.util.DateUtils
import com.example.groove.util.createMapper
import com.example.groove.util.get
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession

@Service
class RemotePlaySocketHandler(
		@Lazy private val socket: WebSocket,
		private val userRepository: UserRepository,
		private val deviceService: DeviceService,
		private val trackService: TrackService
) : SocketHandler<RemotePlayRequest> {

	private val objectMapper = createMapper()

	override fun handleMessage(session: WebSocketSession, data: RemotePlayRequest) {
		val user = userRepository.get(session.userId)!!
		val targetDeviceId = data.targetDeviceId

		val targetDevice = deviceService.getDeviceById(targetDeviceId)

		if (!targetDevice.canBePlayedBy(user.id)) {
			throw PermissionDeniedException("Not authorized to access device")
		}

		val trackIdToTrack = trackService
				.getTracksByIds(data.trackIds?.toSet() ?: emptySet(), user)
				// Don't allow playing your own private songs to someone who isn't you. It won't load for them anyway
				.map { it.id to it }
				.toMap()

		require(trackIdToTrack.values.all { !it.private || targetDevice.user.id == user.id }) {
			"Private tracks may not be played remotely to another user"
		}

		// A user could, theoretically, tell us to play a single track ID more than once.
		// So load all the unique tracks belonging to the IDs from the DB, and then iterate
		// over the IDs we are given so we preserve any duplicate IDs
		val tracksToPlay = data.trackIds?.map { trackIdToTrack.getValue(it) }

		val remotePlayResponse = RemotePlayResponse(
				tracks = tracksToPlay,
				newFloatValue = data.newFloatValue,
				remotePlayAction = data.remotePlayAction
		)

		val targetSession = socket.sessions.values.find { it.deviceIdentifier == targetDevice.deviceId }
				?: throw IllegalStateException("No session exists with device identifier ${targetDevice.deviceId}!")

		targetSession.sendIfOpen(objectMapper.writeValueAsString(remotePlayResponse))
	}

	private fun Device.canBePlayedBy(userId: Long): Boolean {
		// If we are the user, then we're always good
		if (userId == user.id) {
			return true
		}

		// Check if we're in a valid party mode. If we aren't, then this isn't playable by other people
		if (partyEnabledUntil == null || partyEnabledUntil!! < DateUtils.now()) {
			return false
		}

		// We're in a valid party mode. Make sure the user who is controlling us is present in the list
		return partyUsers.any { it.id == userId }
	}

}

data class RemotePlayRequest(
		override val messageType: EventType,
		val deviceId: String,
		val targetDeviceId: Long,
		val trackIds: List<Long>?,
		val newFloatValue: Double?,
		val remotePlayAction: RemotePlayType
) : WebSocketMessage

data class RemotePlayResponse(
		override val messageType: EventType = EventType.REMOTE_PLAY,
		val tracks: List<Track>?,
		val newFloatValue: Double?,
		val remotePlayAction: RemotePlayType
) : WebSocketMessage
