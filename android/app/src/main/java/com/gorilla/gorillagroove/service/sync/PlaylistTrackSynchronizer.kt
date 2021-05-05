package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbPlaylistTrack
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.di.Network
import java.time.Instant

object PlaylistTrackSynchronizer : StandardSynchronizer<DbPlaylistTrack, PlaylistTrackResponse>({ GorillaDatabase.playlistTrackDao }) {
    override suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<PlaylistTrackResponse> {
        return Network.api.getPlaylistTrackSyncEntities(
            minimum = syncStatus.lastSynced?.toEpochMilli() ?: 0,
            maximum = maximum.toEpochMilli(),
            page = page
        )
    }

    override suspend fun convertToDatabaseEntity(networkEntity: PlaylistTrackResponse) = networkEntity.asPlaylistTrack()
}

data class PlaylistTrackResponse(
    override val id: Long,
    val track: PlaylistTrackTrackResponse,
    val playlistId: Long,
    val sortOrder: Int,
    val createdAt: Instant,
) : EntityResponse {
    fun asPlaylistTrack() = DbPlaylistTrack(
        id = id,
        trackId = track.id,
        playlistId = playlistId,
        sortOrder = sortOrder,
        createdAt = createdAt,
    )
}

data class PlaylistTrackTrackResponse(val id: Long)
