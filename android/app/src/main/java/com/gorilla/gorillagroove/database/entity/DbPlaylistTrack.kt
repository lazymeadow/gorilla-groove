package com.gorilla.gorillagroove.database.entity

import androidx.room.*
import java.time.Instant

@Entity(tableName = "playlist_track")
data class DbPlaylistTrack(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "track_id")
    var trackId: Long,

    @ColumnInfo(name = "playlist_id")
    var playlistId: Long,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant,

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int,
) : DbEntity
