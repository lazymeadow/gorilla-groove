package com.gorilla.gorillagroove.database.entity

import androidx.room.*
import java.time.Instant

@Entity(tableName = "sync_status")
@TypeConverters(SyncTypeConverter::class)
data class DbSyncStatus(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "sync_type")
    var syncType: SyncType,

    @ColumnInfo(name = "last_synced")
    var lastSynced: Instant?,

    @ColumnInfo(name = "last_synced_attempted")
    var lastSyncAttempted: Instant?,
)

enum class SyncType {
    TRACK,
    PLAYLIST_TRACK,
    PLAYLIST,
    USER,
    REVIEW_SOURCE
    ;
}

object SyncTypeConverter {
    @TypeConverter
    fun fromEnum(type: SyncType): String {
        return type.name
    }

    @TypeConverter
    fun fromInt(name: String): SyncType {
        return SyncType.values().find { it.name == name }
            ?: throw IllegalArgumentException("No sync type defined with name '$name'!")
    }
}
