package com.example.groove.dto

import org.springframework.web.multipart.MultipartFile

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
		val albumArt: MultipartFile?
)
