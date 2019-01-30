package com.example.groove.dto

data class UpdateTrackDTO(
		val trackId: Long,
		val name: String?,
		val artist: String?,
		val album: String?,
		val trackNumber: Int?,
		val releaseYear: Int?,
		val genre: String?,
		val note: String?
)
