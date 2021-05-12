package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.entity.DbUser

@Dao
abstract class UserDao : BaseRoomDao<DbUser>("user") {
    @Query("""
        SELECT *
        FROM user
        WHERE id <> :excludedUserId
        ORDER BY name COLLATE NOCASE ASC
    """)
    abstract fun getOtherUsers(excludedUserId: Long = GGApplication.loggedInUserId!!): List<DbUser>
}
