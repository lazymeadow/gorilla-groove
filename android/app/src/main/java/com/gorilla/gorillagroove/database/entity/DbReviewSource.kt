package com.gorilla.gorillagroove.database.entity

import androidx.room.*

@Entity(tableName = "review_source")
@TypeConverters(ReviewSourceTypeConverter::class, OfflineAvailabilityTypeConverter::class)
data class DbReviewSource(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "source_type")
    var sourceType: ReviewSourceType,

    @ColumnInfo(name = "display_name")
    var displayName: String,

    @ColumnInfo(name = "offline_availability")
    var offlineAvailability: OfflineAvailabilityType,

    @ColumnInfo(name = "active")
    var active: Boolean,
) : DbEntity

enum class ReviewSourceType {
    USER_RECOMMEND,
    YOUTUBE_CHANNEL,
    ARTIST,
    UNKNOWN
    ;
}

class ReviewSourceTypeConverter {
    @TypeConverter
    fun fromEnum(typeReview: ReviewSourceType): String {
        return typeReview.name
    }

    @TypeConverter
    fun fromInt(name: String): ReviewSourceType {
        return ReviewSourceType.values().find { it.name == name } ?: ReviewSourceType.UNKNOWN
    }
}
