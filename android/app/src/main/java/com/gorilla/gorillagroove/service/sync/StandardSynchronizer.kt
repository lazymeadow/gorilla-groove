package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.dao.BaseRoomDao
import com.gorilla.gorillagroove.database.entity.DbEntity
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import java.time.Instant

abstract class StandardSynchronizer<DbType : DbEntity, ResponseType: EntityResponse>(
    private val dao: BaseRoomDao<DbType>,
) {

    suspend fun sync(
        syncStatus: DbSyncStatus,
        maximum: Instant
    ): Boolean {
        var currentPage = 0
        var pagesToGet: Int

        do {
            val (pageToFetch, success) = savePageOfChanges(syncStatus, maximum, currentPage)
            if (!success) {
                return false
            }

            pagesToGet = pageToFetch
            currentPage++
        } while (currentPage < pagesToGet)

        return true
    }

    abstract suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<ResponseType>

    abstract suspend fun convertToDatabaseEntity(networkEntity: ResponseType): DbType

    protected open fun onEntityUpdate(entity: DbType) {}

    private suspend fun savePageOfChanges(syncStatus: DbSyncStatus, maximum: Instant, page: Int): Pair<Int, Boolean> {
        logDebug("Syncing $syncStatus page $page")
        val response = try {
            fetchEntities(syncStatus, maximum, page)
        } catch (e: Throwable) {
            logError("Could not sync page $page of ${syncStatus.syncType}!", e)
            return -1 to false
        }

        val pagesToGet = response.pageable.totalPages

        val content = response.content

        val newDbEntities = content.new.map { convertToDatabaseEntity(it) }
        val updatedDbEntities = content.new.map {
            val entity = convertToDatabaseEntity(it)

            onEntityUpdate(entity)

            entity
        }

        val entitiesToSave = newDbEntities + updatedDbEntities

        if (entitiesToSave.isNotEmpty()) {
            logDebug("Saving ${syncStatus.syncType} entities with IDs: ${entitiesToSave.map { it.id }}")

            dao.save(newDbEntities + updatedDbEntities)
        }

        val idsToDelete = content.removed
        if (idsToDelete.isNotEmpty()) {
            logDebug("Deleting ${syncStatus.syncType} entities with IDs: $idsToDelete")

            dao.delete(idsToDelete)
        }

        return pagesToGet to true
    }
}

interface EntityResponse {
    val id: Long
}
