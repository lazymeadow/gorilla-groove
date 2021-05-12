package com.gorilla.gorillagroove.database.entity

import androidx.room.*
import com.gorilla.gorillagroove.ui.TrackReturnable
import java.io.Serializable
import java.time.Instant

@Entity(tableName = "track")
data class DbTrack(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "artist")
    var artist: String,

    @ColumnInfo(name = "featuring")
    var featuring: String,

    @ColumnInfo(name = "album")
    var album: String,

    @ColumnInfo(name = "track_number")
    var trackNumber: Int?,

    @ColumnInfo(name = "length")
    var length: Int,

    @ColumnInfo(name = "release_year")
    var releaseYear: Int?,

    @ColumnInfo(name = "genre")
    var genre: String?,

    @ColumnInfo(name = "play_count")
    var playCount: Int,

    @ColumnInfo(name = "is_private")
    var thePrivate: Boolean,

    @ColumnInfo(name = "is_hidden")
    var hidden: Boolean,

    @ColumnInfo(name = "added_to_library")
    var addedToLibrary: Instant?,

    @ColumnInfo(name = "last_played")
    var lastPlayed: Instant?,

    @ColumnInfo(name = "in_review")
    var inReview: Boolean,

    @ColumnInfo(name = "note")
    var note: String?,

    @ColumnInfo(name = "song_cached_at")
    var songCachedAt: Instant? = null,

    @ColumnInfo(name = "art_cached_at")
    var artCachedAt: Instant? = null,

    @ColumnInfo(name = "thumbnail_cached_at")
    var thumbnailCachedAt: Instant? = null,

    @ColumnInfo(name = "offline_availability")
    var offlineAvailability: OfflineAvailabilityType,

    @ColumnInfo(name = "filesize_audio_ogg")
    var filesizeAudio: Int,

    @ColumnInfo(name = "filesize_art_png")
    var filesizeArt: Int,

    @ColumnInfo(name = "filesize_thumbnail_png")
    var filesizeThumbnail: Int,

    @ColumnInfo(name = "started_on_device")
    var startedOnDevice: Instant? = null,

    @ColumnInfo(name = "review_source_id")
    var reviewSourceId: Long?,

    @ColumnInfo(name = "last_reviewed")
    var lastReviewed: Instant?,
) : DbEntity, Serializable, TrackReturnable {

    // When we sync from the API, we don't want to stomp out ALL our data, but we do want to stomp out most of it.
    // This preserves fields we want to make sure survive the update. If I end up doing this in more places, it could
    // be cool to maybe make a custom annotation like @NotApiMapped to do this automatically
    fun updateApiEntity(dbEntity: DbTrack) {
        songCachedAt = dbEntity.songCachedAt
        artCachedAt = dbEntity.artCachedAt
        thumbnailCachedAt = dbEntity.thumbnailCachedAt
        startedOnDevice = dbEntity.startedOnDevice
    }

    val artistString: String
    get() {
        val featuringString = if (featuring.isNotBlank()) "ft. $featuring" else ""

        return "$artist $featuringString".trim()
    }

    override fun asTrack() = this
}

enum class OfflineAvailabilityType {
    NORMAL,
    AVAILABLE_OFFLINE,
    ONLINE_ONLY,
    UNKNOWN
    ;
}

class OfflineAvailabilityTypeConverter {
    @TypeConverter
    fun fromEnum(type: OfflineAvailabilityType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromName(name: String?): OfflineAvailabilityType? {
        if (name == null) {
            return null
        }

        // It is possible the API could add a new type, so we don't want to fail if we don't recognize something
        return OfflineAvailabilityType.values().find { it.name == name } ?: OfflineAvailabilityType.UNKNOWN
    }
}
