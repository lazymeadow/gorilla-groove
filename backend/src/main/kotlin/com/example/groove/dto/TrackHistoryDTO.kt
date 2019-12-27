package com.example.groove.dto

import java.sql.Timestamp

data class TrackHistoryDTO(
		val trackHistoryId: Long,
		val listenedDate: Timestamp,
		val trackName: String,
		val trackArtist: String,
		val trackAlbum: String,
		val trackLength: Int // I strongly suspect this will be returning more data in the future.
		                     // But only adding what I need for right now
)