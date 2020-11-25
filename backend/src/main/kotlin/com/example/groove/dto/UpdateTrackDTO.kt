package com.example.groove.dto

import com.example.groove.db.model.enums.OfflineAvailabilityType

data class UpdateTrackDTO(
		val trackIds: List<Long>,
		val name: String?,
		val artist: String?,
		val featuring: String?,
		val album: String?,
		val trackNumber: Int?,
		val releaseYear: Int?,
		val genre: String?,
		val note: String?,
		val hidden: Boolean?,
		val albumArtUrl: String?,
		val offlineAvailability: OfflineAvailabilityType?,
		val cropArtToSquare: Boolean = false
)
