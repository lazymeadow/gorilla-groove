package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbReviewSource

@Dao
abstract class ReviewSourceDao : BaseRoomDao<DbReviewSource>("review_source")
