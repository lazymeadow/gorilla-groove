package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.di.Network
import java.time.Instant

object PlaylistSynchronizer : StandardSynchronizer<DbPlaylist, PlaylistResponse>(GorillaDatabase.playlistDao) {
    override suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<PlaylistResponse> {
        return Network.api.getPlaylistSyncEntities(
            minimum = syncStatus.lastSynced?.toEpochMilli() ?: 0,
            maximum = maximum.toEpochMilli(),
            page = page
        )
    }

    override suspend fun convertToDatabaseEntity(networkEntity: PlaylistResponse) = networkEntity.asPlaylist()
}

data class PlaylistResponse(
    override val id: Long,
    val name: String,
    val updatedAt: Instant,
    val createdAt: Instant,
) : EntityResponse {
    fun asPlaylist() = DbPlaylist(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
