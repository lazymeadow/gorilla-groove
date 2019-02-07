package com.example.groove.dto

data class YoutubeDownloadDTO(
		val url: String,
		val name: String?,
		val artist: String?,
		val featuring: String?,
		val album: String?,
		val releaseYear: Int?,
		val trackNumber: Int?,
		val genre: String?
)
