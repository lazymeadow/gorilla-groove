package com.example.groove.dto

data class UpdateTrackDTO(
		val trackId: Long,
		val name: String?,
		val artist: String?,
		val album: String?,
		val releaseYear: Int?,
		val trackNumber: Int?,
		val note: String?
)
