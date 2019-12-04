package com.example.groove.dto

import com.example.groove.db.model.Track

data class TrackChangesDTO(
		val newTracks: List<Track>,
		val modifiedTracks: List<Track>,
		val removedTrackIds: List<Long>
)