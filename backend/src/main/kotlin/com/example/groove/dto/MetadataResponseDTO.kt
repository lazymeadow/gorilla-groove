package com.example.groove.dto

data class MetadataResponseDTO (
	val name: String,
	val artist: String,
	val album: String,
	val genre: String,
	val releaseYear: Int,
	val trackNumber: Int,
	val albumArtLink: String
) {
	// Add an empty companion object so private extensions can be added
	companion object
}