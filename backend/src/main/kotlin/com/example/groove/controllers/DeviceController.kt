package com.example.groove.controllers

import com.example.groove.config.WebSocketConfig
import com.example.groove.db.model.Device
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.services.DeviceService
import com.example.groove.services.UserService
import com.example.groove.services.event.EventServiceCoordinator
import com.example.groove.util.loadLoggedInUser

import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("api/device")
class DeviceController(
		private val deviceService: DeviceService,
		private val userService: UserService,
		private val webSocketConfig: WebSocketConfig
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
		return webSocketConfig.getActiveDevices(excludingDeviceId)
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
	@PutMapping
	fun updateDeviceVersion(
			@RequestBody body: UpdateDeviceVersionDTO,
			request: HttpServletRequest
	) {
		val ipAddress = request.getHeader("x-forwarded-for")

		deviceService.createOrUpdateDevice(
				user = loadLoggedInUser(),
				deviceId = body.deviceId,
				deviceType = body.deviceType,
				version = body.version,
				ipAddress = ipAddress,
				additionalData = body.additionalData
		)

		// This basically is a "log in". Update our last login time here
		userService.updateOwnLastLogin()
	}

	data class UpdateDeviceDTO(val deviceName: String)

	data class ArchiveDeviceDTO(val archived: Boolean)

	data class MergeDevicesDTO(
			val id: Long,
			val targetId: Long
	)

	data class UpdateDeviceVersionDTO(
			val deviceId: String,
			val deviceType: DeviceType,
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
}
