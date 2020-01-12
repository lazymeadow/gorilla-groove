package com.example.groove.dto

import com.example.groove.db.model.Track

data class TrackChangesDTO(
		val newTracks: List<Track>,
		val modifiedTracks: List<Track>,
		val removedTrackIds: List<Long>
)

data class PageResponseDTO(
		val offset: Long,
		val pageSize: Int,
		val pageNumber: Int,
		val totalPages: Int,
		val totalElements: Long
)

data class TrackChangesResponseDTO(
		val content: TrackChangesDTO,
		val pageable: PageResponseDTO
)
