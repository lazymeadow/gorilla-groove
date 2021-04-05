package com.gorilla.gorillagroove.network.track

import com.gorilla.gorillagroove.service.sync.TrackResponse

data class TrackUpdate(
    val trackIds: List<Long>,
    val name: String?,
    val artist: String?,
    val featuring: String?,
    val album: String?,
    val trackNumber: Int?,
    val releaseYear: Int?,
    val genre: String?,
    val note: String?,
    val hidden: Boolean?,
    val albumArtUrl: String?,
    val cropArtToSquare: Boolean?
)

data class MultiTrackResponse(
    val items: List<TrackResponse>
)

data class MarkListenedRequest(
    val trackId: Long,
    val timeListenedAt: String,
    val ianaTimezone: String,
    val latitude: Double?,
    val longitude: Double?
)
