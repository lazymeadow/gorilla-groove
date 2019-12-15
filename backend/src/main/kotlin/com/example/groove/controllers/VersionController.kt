package com.example.groove.controllers

import com.example.groove.properties.VersionProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/version")
class VersionController(
		private val versionProperties: VersionProperties
) {

	@GetMapping
	fun getVersion(): ResponseEntity<Map<String, String>> {
		return ResponseEntity.ok(mapOf("version" to versionProperties.version!!))
	}
}
