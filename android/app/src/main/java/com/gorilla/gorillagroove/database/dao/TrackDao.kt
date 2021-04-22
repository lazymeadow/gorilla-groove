package com.gorilla.gorillagroove.database.dao

import android.graphics.Bitmap
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.ui.menu.SortDirection


@Dao
abstract class TrackDao : BaseRoomDao<DbTrack>("track") {

    fun findTracksWithSort(
        inReview: Boolean = false,
        isHidden: Boolean? = null,
        albumFilter: String? = null,
        sortType: TrackSortType,
        sortDirection: SortDirection
    ): List<DbTrack> {
        if (sortDirection == SortDirection.NONE) {
            throw IllegalArgumentException("Cannot sort by direction: NONE")
        }

        val statement = """
            SELECT * 
            FROM track 
            WHERE in_review = ${inReview.toInt()}
            AND (${isHidden?.toInt()} IS NULL OR is_hidden = ${isHidden?.toInt()})
            AND (${albumFilter?.let { 1 }} IS NULL OR album = '${albumFilter?.sqlEscaped()}')
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
        AND song_cached_at IS NULL
 """)
    abstract fun getTracksNeedingCached(): List<DbTrack>

    @Query("""
        SELECT id, album 
        FROM track 
        WHERE in_review = :inReview
        AND (:artistFilter IS NULL OR (artist LIKE '%' || :artistFilter || '%' OR featuring LIKE '%' || :artistFilter || '%'))
        AND (:isHidden IS NULL OR is_hidden = :isHidden)
        GROUP BY album
        ORDER BY album COLLATE NOCASE ASC    
 """)
    abstract fun getDistinctAlbums(
        inReview: Boolean = false,
        isHidden: Boolean? = null,
        artistFilter: String? = null,
    ): List<Album>
}

private const val NO_CASE = "COLLATE NOCASE"
enum class TrackSortType(val trackPropertyName: String, val collation: String = "") {
    NAME("name", NO_CASE),
    PLAY_COUNT("play_count"),
    DATE_ADDED("added_to_library"),
    ALBUM("album", NO_CASE),
    YEAR("release_year");
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
