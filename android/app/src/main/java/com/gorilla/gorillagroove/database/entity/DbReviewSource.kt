package com.gorilla.gorillagroove.database.entity

import androidx.room.*

@Entity(tableName = "review_source")
@TypeConverters(SourceTypeConverter::class, OfflineAvailabilityTypeConverter::class)
data class DbReviewSource(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "source_type")
    var sourceType: SourceType,

    @ColumnInfo(name = "display_name")
    var displayName: String,

    @ColumnInfo(name = "offline_availability")
    var offlineAvailabilityType: OfflineAvailabilityType,

    @ColumnInfo(name = "active")
    var active: Boolean,
)

enum class SourceType {
    USER_RECOMMEND,
    YOUTUBE_CHANNEL,
    ARTIST,
    UNKNOWN
    ;
}

object SourceTypeConverter {
    @TypeConverter
    fun fromEnum(type: SourceType): String {
        return type.name
    }

    @TypeConverter
    fun fromInt(name: String): SourceType {
        return SourceType.values().find { it.name == name } ?: SourceType.UNKNOWN
    }
}
