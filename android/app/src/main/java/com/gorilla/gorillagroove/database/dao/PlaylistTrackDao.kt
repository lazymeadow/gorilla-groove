package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbPlaylistTrack

@Dao
abstract class PlaylistTrackDao : BaseRoomDao<DbPlaylistTrack>("playlist_track")
