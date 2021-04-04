package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbUser

@Dao
abstract class UserDao : BaseRoomDao<DbUser>("user")
