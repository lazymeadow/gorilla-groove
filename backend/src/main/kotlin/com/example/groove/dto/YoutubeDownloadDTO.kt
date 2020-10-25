package com.example.groove.dto

data class YoutubeDownloadDTO(
		val url: String,
		val name: String? = null,
		val artist: String? = null,
		val featuring: String? = null,
		val album: String? = null,
		val releaseYear: Int? = null,
		val trackNumber: Int? = null,
		val genre: String? = null,
		val cropArtToSquare: Boolean = false,
		val artUrl: String? = null
)
