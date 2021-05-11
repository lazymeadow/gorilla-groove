package com.gorilla.gorillagroove.database.dao

import android.graphics.Bitmap
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.ui.menu.SortDirection


@Dao
abstract class TrackDao : BaseRoomDao<DbTrack>("track") {

    fun findTracksWithSort(
        inReview: Boolean = false,
        isHidden: Boolean? = null,
        artistFilter: String? = null,
        albumFilter: String? = null,
        availableOffline: Boolean? = null,
        sortType: TrackSortType = TrackSortType.NAME,
        sortDirection: SortDirection = SortDirection.ASC
    ): List<DbTrack> {
        if (sortDirection == SortDirection.NONE) {
            throw IllegalArgumentException("Cannot sort by direction: NONE")
        }

        val escapedArtist = artistFilter?.sqlEscaped()

        val statement = """
            SELECT * 
            FROM track 
            WHERE in_review = ${inReview.toInt()}
            AND (${isHidden?.toInt()} IS NULL OR is_hidden = ${isHidden?.toInt()})
            AND (
                ${artistFilter?.let { 1 }} IS NULL OR 
                ('$artistFilter' = '' AND artist = '$escapedArtist' AND featuring = '$escapedArtist') OR
                ('$artistFilter' <> '' AND (artist LIKE '$escapedArtist' OR featuring LIKE '$escapedArtist'))
            )
            AND (${albumFilter?.let { 1 }} IS NULL OR album = '${albumFilter?.sqlEscaped()}')
            AND ($availableOffline IS NULL OR (
                (${availableOffline?.toInt()} = 1 AND song_cached_at IS NOT NULL)
                OR (${availableOffline?.toInt()} = 0 AND song_cached_at IS NULL)
            ))
            ORDER BY ${sortType.trackPropertyName} ${sortType.collation} ${sortDirection.name}
        """.trimIndent()

        return executeSqlWithReturn(SimpleSQLiteQuery(statement))
    }

    @Query("""
        SELECT artist 
            FROM track 
            WHERE in_review = :inReview
            AND (:isHidden IS NULL OR is_hidden = :isHidden)
        UNION SELECT featuring 
            FROM track
            WHERE in_review = :inReview
            AND (:isHidden IS NULL OR is_hidden = :isHidden)
        ORDER BY artist COLLATE NOCASE ASC    
 """)
    abstract fun getDistinctArtists(
        inReview: Boolean = false,
        isHidden: Boolean? = null,
    ): List<String>

    @Query("""
        SELECT *
        FROM track 
        WHERE offline_availability = 'AVAILABLE_OFFLINE'
        AND (
          song_cached_at IS NULL
          OR (art_cached_at IS NULL AND filesize_art_png > 0)
        )
 """)
    abstract fun getTracksNeedingCached(): List<DbTrack>

    @Query("""
        SELECT id, album 
        FROM track 
        WHERE in_review = :inReview
        AND (
            :artistFilter IS NULL OR 
            (:artistFilter = '' AND artist = :artistFilter AND featuring = :artistFilter) OR
            (:artistFilter <> '' AND (artist LIKE :artistFilter OR featuring LIKE :artistFilter))
        )
        AND (:isHidden IS NULL OR is_hidden = :isHidden)
        GROUP BY album
        ORDER BY album COLLATE NOCASE ASC    
 """)
    abstract fun getDistinctAlbums(
        inReview: Boolean = false,
        isHidden: Boolean? = null,
        artistFilter: String? = null,
    ): List<Album>

    // This can be simplified to use IFF() when Android lets us use SQLite 3.32.0
    // I'm not including thumbnails here because they're too small to be worth the complication
    @Query("""
        SELECT sum(
          CASE song_cached_at IS NULL
             WHEN 1 THEN 0
             ELSE filesize_audio_ogg
          END
        )
        +
        sum(
          CASE art_cached_at IS NULL
             WHEN 1 THEN 0
             ELSE filesize_art_png
          END
        )
        AS bytes
        FROM track
        WHERE (:offlineAvailabilityType IS NULL OR offline_availability = :offlineAvailabilityType)
 """)
    abstract fun getCachedTrackSizeBytes(offlineAvailabilityType: OfflineAvailabilityType? = null): Long

    @Query("""
        SELECT sum(filesize_art_png) + sum(filesize_audio_ogg) AS bytes
        FROM track
        WHERE offline_availability = 'AVAILABLE_OFFLINE'
; 
 """)
    abstract fun getTotalBytesRequiredForFullCache(): Long

    @Query("""
        SELECT *
        FROM track
        WHERE offline_availability = :offlineAvailabilityType
        AND (song_cached_at IS NOT NULL OR art_cached_at IS NOT NULL)
        ORDER BY started_on_device ASC
 """)
    abstract fun getCachedTrackByOfflineTypeSortedByOldestStarted(offlineAvailabilityType: OfflineAvailabilityType): List<DbTrack>

    @Query("""
        SELECT count(*)
        FROM track
        WHERE (:offlineAvailabilityType IS NULL OR offline_availability = :offlineAvailabilityType)
        AND (
            (:isCached IS NULL OR (:isCached = 1 AND song_cached_at IS NOT NULL)) 
            OR (:isCached IS NULL OR (:isCached = 0 AND song_cached_at IS NULL)) 
        )
 """)
    abstract fun getTrackCount(
        offlineAvailabilityType: OfflineAvailabilityType? = null,
        isCached: Boolean? = null
    ): Int

    @Query("""
        SELECT *
        FROM track
        WHERE review_source_id = :reviewSourceId
        AND in_review = 1
 """)
    abstract fun getTracksNeedingReviewOnSource(
        reviewSourceId: Long,
    ): List<DbTrack>

    @Query("""
        DELETE
        FROM track
        WHERE review_source_id = :reviewSourceId
 """)
    abstract fun deleteTracksOnReviewSource(
        reviewSourceId: Long,
    ): Int
}

private const val NO_CASE = "COLLATE NOCASE"
enum class TrackSortType(val trackPropertyName: String, val apiPropertyName: String, val collation: String = "") {
    NAME("name", "name", NO_CASE),
    PLAY_COUNT("play_count", "playCount"),
    DATE_ADDED("added_to_library", "addedToLibrary"),
    ALBUM("album", "album", NO_CASE),
    YEAR("release_year", "releaseYear");
}

fun Boolean.toInt() = if (this) 1 else 0

data class Album(
    @ColumnInfo(name = "album")
    val name: String,

    @ColumnInfo(name = "id")
    val trackId: Long,
) {
    @Ignore
    var imageData: Bitmap? = null

    @Ignore
    var albumArtFetched: Boolean = false
}
