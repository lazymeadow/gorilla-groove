package com.example.groove.controllers

import com.example.groove.db.model.VersionHistory
import com.example.groove.db.model.enums.DeviceType
import com.example.groove.properties.VersionProperties
import com.example.groove.services.VersionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/version")
class VersionController(
		private val versionProperties: VersionProperties,
		private val versionService: VersionService
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
}
