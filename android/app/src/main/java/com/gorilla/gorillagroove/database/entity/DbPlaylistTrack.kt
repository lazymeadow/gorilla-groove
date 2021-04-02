package com.gorilla.gorillagroove.database.entity

import androidx.room.*

@Entity(tableName = "playlist_track")
data class DbPlaylistTrack(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "track_id")
    var trackId: Long,

    @ColumnInfo(name = "playlist_id")
    var playlistId: Long,

    @ColumnInfo(name = "created_at")
    var createdAt: Long,

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int,
)
