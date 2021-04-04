package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.dao.ReviewSourceDao
import com.gorilla.gorillagroove.database.entity.DbReviewSource
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.database.entity.ReviewSourceType
import com.gorilla.gorillagroove.network.NetworkApi
import java.time.Instant

class ReviewSourceSynchronizer(
    private val networkApi: NetworkApi,
    reviewSourceDao: ReviewSourceDao
) : StandardSynchronizer<DbReviewSource, ReviewSourceResponse>(reviewSourceDao) {
    override suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<ReviewSourceResponse> {
        return networkApi.getReviewSourceSyncEntities(
            minimum = syncStatus.lastSynced?.toEpochMilli() ?: 0,
            maximum = maximum.toEpochMilli(),
            page = page
        )
    }

    override suspend fun convertToDatabaseEntity(networkEntity: ReviewSourceResponse) = networkEntity.asReviewSource()
}

data class ReviewSourceResponse(
    override val id: Long,
    val displayName: String,
    val sourceType: ReviewSourceType,
    val offlineAvailabilityType: OfflineAvailabilityType,
    val active: Boolean,
) : EntityResponse {
    fun asReviewSource() = DbReviewSource(
        id = id,
        displayName = displayName,
        sourceType = sourceType,
        offlineAvailability = offlineAvailabilityType,
        active = active
    )
}
