package com.example.groove.dto

data class YoutubeDownloadDTO(
		val url: String,
		val name: String?,
		val artist: String?,
		val album: String?,
		val releaseYear: Int?
)
