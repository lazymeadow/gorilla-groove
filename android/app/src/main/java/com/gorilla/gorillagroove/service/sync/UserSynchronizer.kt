package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.di.Network
import java.time.Instant

object UserSynchronizer : StandardSynchronizer<DbUser, UserResponse>(GorillaDatabase.userDao) {
    override suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<UserResponse> {
        return Network.api.getUserSyncEntities(
            minimum = syncStatus.lastSynced?.toEpochMilli() ?: 0,
            maximum = maximum.toEpochMilli(),
            page = page
        )
    }

    override suspend fun convertToDatabaseEntity(networkEntity: UserResponse) = networkEntity.asUser()
}

data class UserResponse(
    override val id: Long,
    val name: String,
    val lastLogin: Instant?,
    val createdAt: Instant,
) : EntityResponse {
    fun asUser() = DbUser(
        id = id,
        name = name,
        lastLogin = lastLogin,
        createdAt = createdAt
    )
}
