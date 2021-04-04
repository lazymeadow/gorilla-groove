package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbTrack

@Dao
abstract class TrackDao : BaseRoomDao<DbTrack>("track")
