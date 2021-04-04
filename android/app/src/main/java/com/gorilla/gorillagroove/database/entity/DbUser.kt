package com.gorilla.gorillagroove.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "user")
data class DbUser(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "last_login")
    var lastLogin: Instant?,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant,
) : DbEntity
