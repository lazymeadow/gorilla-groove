package com.example.groove.dto

import java.sql.Timestamp

data class TrackHistoryDTO(
		val id: Long,
		val listenedDate: Timestamp,
		val trackName: String,
		val trackArtist: String,
		val trackAlbum: String,
		val trackLength: Int,
		val deviceName: String?
)