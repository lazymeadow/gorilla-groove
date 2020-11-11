package com.example.groove.dto

import com.example.groove.db.model.Track
import com.example.groove.services.enums.MetadataOverrideType

data class MetadataResponseDTO (
		val sourceId: String,
		val name: String,
		val artist: String,
		val album: String,
		val releaseYear: Int,
		val trackNumber: Int,
		val albumArtLink: String,
		val length: Int,
		val previewUrl: String? // Not all tracks have this. Quite a few don't, actually
) {
	// Add an empty companion object so private extensions can be added
	companion object
}

data class MetadataUpdateRequestDTO(
		val trackIds: List<Long>,
		val changeAlbum: MetadataOverrideType = MetadataOverrideType.NEVER,
		val changeAlbumArt: MetadataOverrideType = MetadataOverrideType.NEVER,
		val changeReleaseYear: MetadataOverrideType = MetadataOverrideType.NEVER,
		val changeTrackNumber: MetadataOverrideType = MetadataOverrideType.NEVER
)

data class DataUpdateResponseDTO(
		val successfulUpdates: List<Track>,
		val failedUpdateIds: List<Long>
)
