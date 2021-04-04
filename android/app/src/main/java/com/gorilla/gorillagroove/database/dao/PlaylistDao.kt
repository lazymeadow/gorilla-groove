package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbPlaylist

@Dao
abstract class PlaylistDao : BaseRoomDao<DbPlaylist>("playlist")
