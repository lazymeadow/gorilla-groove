package com.example.groove.dto

import com.example.groove.db.model.Track
import com.example.groove.services.enums.MetadataOverrideType
import java.awt.Image

data class MetadataResponseDTO (
		val name: String,
		val artist: String,
		val album: String,
		val genre: String? = null, // Not returned by Spotify
		val releaseYear: Int,
		val trackNumber: Int,
		val albumArtUrl: String,
		val songLength: Int
) {
	// Add an empty companion object so private extensions can be added
	companion object
}

data class MetadataUpdateRequestDTO(
		val trackIds: List<Long>,
		val changeAlbum: MetadataOverrideType = MetadataOverrideType.NEVER,
		val changeAlbumArt: MetadataOverrideType = MetadataOverrideType.NEVER,
		val changeGenre: MetadataOverrideType = MetadataOverrideType.NEVER,
		val changeReleaseYear: MetadataOverrideType = MetadataOverrideType.NEVER,
		val changeTrackNumber: MetadataOverrideType = MetadataOverrideType.NEVER
)

data class DataUpdateResponseDTO(
		val successfulUpdates: List<Track>,
		val failedUpdateIds: List<Long>
)
