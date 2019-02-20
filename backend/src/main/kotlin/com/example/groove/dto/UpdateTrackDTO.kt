package com.example.groove.dto

data class UpdateTrackDTO(
		val trackIds: List<Long>,
		val name: String?,
		val artist: String?,
		val featuring: String?,
		val album: String?,
		val trackNumber: Int?,
		val releaseYear: Int?,
		val genre: String?,
		val note: String?
)
