package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbPlaylistTrack
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.ui.TrackReturnable

@Dao
abstract class PlaylistTrackDao : BaseRoomDao<DbPlaylistTrack>("playlist_track") {
    // Because playlist tracks and tracks both have duplicate column names (specifically "id"), we have to alias one of the selects.
    // Unfortunately this means we can't just do "SELECT t.*, pt.*" and have to reference every column. Sucks.
    @Query("""
        SELECT t.*,
          pt.id AS pt_id,
          pt.track_id AS pt_track_id,
          pt.playlist_id AS pt_playlist_id,
          pt.created_at AS pt_created_at,
          pt.sort_order AS pt_sort_order
        FROM playlist_track pt
        JOIN track t
          ON pt.track_id = t.id
        WHERE pt.playlist_id = :playlistId
        ORDER BY pt.sort_order ASC
    """)
    abstract fun findTracksOnPlaylist(playlistId: Long): List<PlaylistTrackWithTrack>
}

data class PlaylistTrackWithTrack(
    @Embedded(prefix = "pt_")
    val playlistTrack: DbPlaylistTrack,

    @Embedded
    val track: DbTrack,
): TrackReturnable {
    override fun asTrack() = track
}
