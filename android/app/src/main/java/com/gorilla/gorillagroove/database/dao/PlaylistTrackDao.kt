package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbPlaylistTrack
import com.gorilla.gorillagroove.database.entity.DbTrack

@Dao
abstract class PlaylistTrackDao : BaseRoomDao<DbPlaylistTrack>("playlist_track") {
    @Query("""
        SELECT t.* 
        FROM playlist_track pt
        JOIN track t
          ON pt.track_id = t.id
        WHERE pt.playlist_id = :playlistId
    """)
    abstract fun findTracksOnPlaylist(playlistId: Long): List<DbTrack>
}
