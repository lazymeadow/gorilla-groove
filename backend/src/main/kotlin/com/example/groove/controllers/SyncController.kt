package com.example.groove.controllers

import com.example.groove.db.model.enums.SyncableEntityType
import com.example.groove.dto.*
import com.example.groove.services.SyncableEntityService
import com.example.groove.util.logger

import org.springframework.web.bind.annotation.*
import java.sql.Timestamp

@RestController
@RequestMapping("api/sync")
class SyncController(
		private val syncableEntityService: SyncableEntityService
) {
	// Used by the apps to keep local data in sync
	@GetMapping("/entity-type/{entity-type}/minimum/{minimum}/maximum/{maximum}")
	fun getTracksChangedBetweenTimestamp(
			@PathVariable("entity-type") entityType: SyncableEntityType,
			@PathVariable("minimum") minimum: Long,
			@PathVariable("maximum") maximum: Long,
			@RequestParam(value = "page", defaultValue = "0") page: Int,
			@RequestParam(value = "size", defaultValue = "20") size: Int
	): RemoteSyncResponseDTO {
		return syncableEntityService.getChangesBetweenTimestamp(
				type = entityType,
				minimum = Timestamp(minimum),
				maximum = Timestamp(maximum),
				page = page,
				size = size
		)
	}

	companion object {
		val logger = logger()
	}
}
