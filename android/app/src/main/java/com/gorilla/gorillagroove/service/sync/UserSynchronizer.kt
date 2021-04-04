package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.dao.UserDao
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.network.NetworkApi
import java.time.Instant

class UserSynchronizer(
    private val networkApi: NetworkApi,
    userDao: UserDao,
) : StandardSynchronizer<DbUser, UserResponse>(userDao) {
    override suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<UserResponse> {
        return networkApi.getUserSyncEntities(
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
