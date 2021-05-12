package com.gorilla.gorillagroove.database.entity

import androidx.room.*
import com.gorilla.gorillagroove.service.sync.SyncType
import java.time.Instant

@Entity(tableName = "sync_status")
@TypeConverters(SyncTypeConverter::class)
data class DbSyncStatus(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override val id: Long = 0,

    @ColumnInfo(name = "sync_type")
    var syncType: SyncType,

    @ColumnInfo(name = "last_synced")
    var lastSynced: Instant? = null,

    @ColumnInfo(name = "last_synced_attempted")
    var lastSyncAttempted: Instant?,
) : DbEntity

class SyncTypeConverter {
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
