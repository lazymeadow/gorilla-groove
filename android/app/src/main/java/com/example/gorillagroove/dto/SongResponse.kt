package com.example.gorillagroove.dto

data class SongResponse(
    val id: Long,
    val title: String,
    val artist: String,
    val trackLink: String?,
    val imageUrl: String?
)

data class TrackLinkResponse(
    val songLink: String,
    val albumArtLink: String?
)