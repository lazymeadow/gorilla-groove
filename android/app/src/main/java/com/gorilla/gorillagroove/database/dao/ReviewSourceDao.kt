package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbReviewSource

@Dao
abstract class ReviewSourceDao : BaseRoomDao<DbReviewSource>("review_source") {
    @Query("""
        SELECT rs.id AS id, count(*) AS count
        FROM review_source rs
        LEFT JOIN track t
        ON t.review_source_id = rs.id
        WHERE t.in_review = 1
        GROUP BY rs.id
    """)
    abstract fun getNeedingReviewTrackCountByQueue(): List<ReviewSourceWithCount>

    @Query("""
        SELECT *
        FROM review_source rs
        WHERE rs.source_type <> 'USER_RECOMMEND'
        AND rs.active = 1
        ORDER BY rs.display_name COLLATE NOCASE ASC
    """)
    abstract fun getEditableSources(): List<DbReviewSource>
}

data class ReviewSourceWithCount(
    @ColumnInfo(name = "id")
    val reviewSourceId: Long,

    @ColumnInfo(name = "count")
    val count: Int
)
