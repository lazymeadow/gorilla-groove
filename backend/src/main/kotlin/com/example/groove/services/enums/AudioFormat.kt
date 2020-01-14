package com.example.groove.services.enums

// The ordinal value of these is used for mapping to the DB
// Use caution when editing the order, and only add new ones to the end
enum class AudioFormat(
		val extension: String,
		val s3Bucket: String
) {
	OGG(".ogg", "music"),
	MP3(".mp3", "music-mp3")
}
