package com.example.groove.controllers

import com.example.groove.db.model.VersionHistory
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.db.model.enums.PermissionType
import com.example.groove.properties.VersionProperties
import com.example.groove.services.UserService
import com.example.groove.services.VersionService
import com.example.groove.util.loadLoggedInUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/version")
class VersionController(
		private val versionProperties: VersionProperties,
		private val versionService: VersionService,
		private val userService: UserService
) {

	@GetMapping
	fun getVersion(): ResponseEntity<Map<String, String>> {
		return ResponseEntity.ok(mapOf("version" to versionProperties.version!!))
	}

	@GetMapping("/history/deviceType/{deviceType}")
	fun getVersionHistory(
			@PathVariable("deviceType") deviceType: DeviceType,
			@RequestParam("limit") limit: Int?
	): List<VersionHistory> {
		return versionService.getVersionHistories(deviceType, limit ?: 10)
	}

	@PostMapping("/history/deviceType/{deviceType}")
	fun createVersionHistory(
			@PathVariable("deviceType") deviceType: DeviceType,
			@RequestBody body: CreateVersionHistoryDTO
	): VersionHistory {
		userService.assertPermission(loadLoggedInUser(), PermissionType.WRITE_VERSION_HISTORY)

		return versionService.createVersionHistory(deviceType = deviceType, version = body.version, notes = body.notes)
	}

	data class CreateVersionHistoryDTO(
			val version: String,
			val notes: String
	)
}
