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
	// Spotify can have duplicates that have all the same fields, but a different "sourceId".
	// Idk why this is. All I know is it's dumb. I am going to consider a song to be a duplicate
	// if the name, artist, and length are all the same. You could arguably add "album" as well,
	// but I haven't yet decided if I should filter out things like "greatest hits" albums where
	// the same song shows up multiple times on different albums
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MetadataResponseDTO

		if (name != other.name) return false
		if (artist != other.artist) return false
		if (length != other.length) return false

		return true
	}

	override fun hashCode(): Int {
		var result = name.hashCode()
		result = 31 * result + artist.hashCode()
		result = 31 * result + length
		return result
	}

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
