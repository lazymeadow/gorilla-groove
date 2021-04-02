package com.gorilla.gorillagroove.database.entity

import androidx.room.*

@Entity(tableName = "playlist")
data class DbPlaylist(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "created_at")
    var createdAt: Long,

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long,
)
