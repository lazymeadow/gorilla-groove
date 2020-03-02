package com.example.groove.controllers

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
		private val eventServiceCoordinator: EventServiceCoordinator
) {

	@GetMapping
    fun getDevices(@RequestParam showAll: Boolean = false): List<Device> {
		return deviceService.getDevices(loadLoggedInUser())
				.filter { showAll || (!it.archived && it.mergedDevice == null )}
    }

	// Gives the effective device for a given device ID. So if you sent a device that was merged, you'll get the parent
	@GetMapping("/{deviceId}")
    fun getDevice(@PathVariable("deviceId") deviceId: String): Device {
		return deviceService.getDeviceById(deviceId)
    }

	@GetMapping("/active")
	fun getActiveDevices(
			@RequestParam("excluding-device") excludingDeviceId: String?
	): List<Device> {
		return eventServiceCoordinator.getActiveDevices(excludingDeviceId)
	}

	@PostMapping("/party")
	fun changePartyMode(@RequestBody body: PartyModeDTO): Device {
		return if (body.enabled) {
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
}
