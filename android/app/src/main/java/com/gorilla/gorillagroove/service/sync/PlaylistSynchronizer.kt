package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.dao.PlaylistDao
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.network.NetworkApi
import java.time.Instant

class PlaylistSynchronizer(
    private val networkApi: NetworkApi,
    playlistDao: PlaylistDao
) : StandardSynchronizer<DbPlaylist, PlaylistResponse>(playlistDao) {
    override suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<PlaylistResponse> {
        return networkApi.getPlaylistSyncEntities(
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
