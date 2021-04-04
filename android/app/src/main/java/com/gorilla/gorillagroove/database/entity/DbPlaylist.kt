package com.gorilla.gorillagroove.database.entity

import androidx.room.*
import java.time.Instant

@Entity(tableName = "playlist")
data class DbPlaylist(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant,
) : DbEntity
