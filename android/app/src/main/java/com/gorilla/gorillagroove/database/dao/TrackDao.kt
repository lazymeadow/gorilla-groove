package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.ui.menu.SortDirection


@Dao
abstract class TrackDao : BaseRoomDao<DbTrack>("track") {

    fun findTracksWithSort(
        inReview: Boolean = false,
        isHidden: Boolean? = null,
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
            AND ($isHidden IS NULL OR is_hidden = ${isHidden?.toInt()})
            ORDER BY ${sortType.trackPropertyName} ${sortType.collation} ${sortDirection.name}
        """.trimIndent()

        return executeSqlWithReturn(SimpleSQLiteQuery(statement))
    }
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

