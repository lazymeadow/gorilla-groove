package com.example.groove.controllers

import com.example.groove.db.dao.UserTokenRepository
import com.example.groove.services.socket.WebSocket
import com.example.groove.db.model.Device
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.services.DeviceService
import com.example.groove.services.UserService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger

import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("api/device")
class DeviceController(
		private val deviceService: DeviceService,
		private val userService: UserService,
		private val webSocket: WebSocket,
		private val userTokenRepository: UserTokenRepository
) {

	@GetMapping
	fun getDevices(@RequestParam showAll: Boolean = false): List<DeviceResponseDTO> {
		return deviceService.getDevices(loadLoggedInUser())
				.filter { showAll || (!it.archived && it.mergedDevice == null )}
				.map { it.toResponseDTO() }
	}

	// Gives the effective device for a given device ID. So if you sent a device that was merged, you'll get the parent
	@GetMapping("/{deviceId}")
	fun getDevice(@PathVariable("deviceId") deviceId: String): DeviceResponseDTO {
		return deviceService.getDeviceById(deviceId).toResponseDTO()
	}

	@GetMapping("/active")
	fun getActiveDevices(
			@RequestParam("excluding-device") excludingDeviceId: String?
	): List<DeviceResponseDTO> {
		return webSocket.getActiveDevices(excludingDeviceId)
				.map { it.toResponseDTO() }
	}

	@PostMapping("/party")
	fun changePartyMode(@RequestBody body: PartyModeDTO): DeviceResponseDTO {
		val device = if (body.enabled) {
			val maxTime = Int.MAX_VALUE.toLong() * 1000 // January 2038 (Max MySQL supports for Timestamp column)

			require(body.controllingUserIds.isNotEmpty()) {
				"A party isn't a party without at least one other person!"
			}

			deviceService.enableParty(
					deviceIdentifier = body.deviceIdentifier,
					partyUntil = Timestamp(body.partyUntil ?: maxTime),
					partyUserIds = body.controllingUserIds
			)
		} else {
			deviceService.disableParty(body.deviceIdentifier)
		}

		return device.toResponseDTO()
	}

	@PutMapping("/update/{id}")
	fun updateDevice(
			@PathVariable("id") id: Long,
			@RequestBody body: UpdateDeviceDTO
	) {
		deviceService.updateDevice(id = id, deviceName = body.deviceName)
	}

	@PutMapping("/merge")
	fun mergeDevice(@RequestBody body: MergeDevicesDTO) {
		if (body.id == body.targetId) {
			throw IllegalArgumentException("IDs must be different!")
		}

		deviceService.mergeDevices(id = body.id, targetId = body.targetId)
	}

	@PutMapping("/archive/{id}")
	fun archiveDevice(
			@PathVariable("id") id: Long,
			@RequestBody body: ArchiveDeviceDTO
	) {
		deviceService.archiveDevice(id, body.archived)
	}

	@DeleteMapping("/{id}")
	fun deleteDevice(@PathVariable("id") id: Long) {
		deviceService.deleteDevice(id)
	}

	// Hmm. Probably should have made this not use the base URL... since it's really only for the version
    @Deprecated("This endpoint requires clients to send in their device ID and device type. This is no longer required and we should sunset this endpoint since a rename is good anyway.")
	@PutMapping
	fun updateDeviceVersion(
			@RequestBody body: UpdateDeviceVersionDTO,
			request: HttpServletRequest
	) {
		val ipAddress = request.getHeader("x-forwarded-for")
		val user = loadLoggedInUser()

		deviceService.createOrUpdateDevice(
				user = user,
				deviceId = body.deviceId,
				deviceType = body.deviceType,
				version = body.version,
				ipAddress = ipAddress,
				additionalData = body.additionalData
		)

		// This is TEMPORARY as a migration effort to associate the user's device to their login session so it doesn't
		// have to get passed in everywhere anymore
		if (user.currentAuthToken!!.device == null) {
			user.currentAuthToken!!.device = deviceService.getDeviceById(body.deviceId)
			logger.info("User ${user.name} did not have a device associated with their token of ${user.currentAuthToken!!.token}. " +
					"Setting it to be ${user.currentAuthToken!!.device}")
			userTokenRepository.save(user.currentAuthToken!!)
		}

		// This basically is a "log in". Update our last login time here
		userService.updateOwnLastLogin()
	}

	@PutMapping("/version")
	fun updateDeviceVersionV2(
		@RequestBody body: UpdateDeviceVersionV2DTO,
		request: HttpServletRequest
	) {
		val ipAddress = request.getHeader("x-forwarded-for")
		val user = loadLoggedInUser()

		val device = user.currentAuthToken!!.device ?: run {
			logger.error("User did not have a device associated to their current session!")
			throw IllegalStateException("No auth token associated with device!")
		}

		if (body.version != device.applicationVersion) {
			logger.info("User ${user.name} updated ${device.deviceType} from version ${device.applicationVersion} to ${body.version}")
		}

		deviceService.createOrUpdateDevice(
			user = user,
			deviceId = device.deviceId,
			deviceType = device.deviceType,
			version = body.version,
			ipAddress = ipAddress,
			additionalData = body.additionalData
		)

		// This basically is a "log in", since the clients use this to check in on page refresh / app open
		userService.updateOwnLastLogin()
	}

	data class UpdateDeviceDTO(val deviceName: String)

	data class ArchiveDeviceDTO(val archived: Boolean)

	data class MergeDevicesDTO(
			val id: Long,
			val targetId: Long
	)

    @Deprecated("The endpoint using this is being sunset")
	data class UpdateDeviceVersionDTO(
			val deviceId: String,
			val deviceType: DeviceType,
			val version: String,
			val additionalData: String?
	)

	data class UpdateDeviceVersionV2DTO(
		val version: String,
		val additionalData: String?
	)

	data class PartyModeDTO(
			val deviceIdentifier: String,
			val enabled: Boolean,
			val partyUntil: Long?,
			val controllingUserIds: Set<Long> = emptySet()
	)

	data class DeviceResponseDTO(
			val id: Long,
			val userId: Long,
			val userName: String,
			val deviceType: DeviceType,
			val deviceId: String,
			val deviceName: String,
			val applicationVersion: String,
			val lastIp: String,
			val additionalData: String? = null,
			val partyEnabledUntil: Timestamp? = null,
			val createdAt: Timestamp,
			val updatedAt: Timestamp
	)

	fun Device.toResponseDTO() = DeviceResponseDTO(
			id = id,
			userId = user.id,
			userName = user.name,
			deviceType = deviceType,
			deviceId = deviceId,
			deviceName = deviceName,
			applicationVersion = applicationVersion,
			lastIp = lastIp,
			additionalData = additionalData,
			partyEnabledUntil = partyEnabledUntil,
			createdAt = createdAt,
			updatedAt = updatedAt
	)

	companion object {
		val logger = logger()
	}
}
