package com.gorilla.gorillagroove.service

import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.ChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

object TrackService {
    suspend fun deleteTracks(tracks: List<DbTrack>): Boolean = withContext(Dispatchers.IO) {
        val trackIds = tracks.map { it.id }

        logInfo("Deleting tracks: $trackIds")

        try {
            Network.api.deleteTracks(trackIds)
        } catch (e: Throwable) {
            logError("Failed to delete tracks!", e)
            return@withContext false
        }

        logInfo("Tracks were deleted remotely. Deleting local state")
        GorillaDatabase.trackDao.delete(trackIds)

        tracks.forEach { track ->
            TrackCacheService.deleteAllCacheOnDisk(track.id)
        }

        broadcastTrackChange(tracks, ChangeType.DELETED)

        return@withContext true
    }

    fun broadcastTrackChange(tracks: List<DbTrack>, changeType: ChangeType) {
        EventBus.getDefault().post(TrackChangeEvent(tracks, changeType))
    }
}

data class TrackChangeEvent(
    val tracks: List<DbTrack>,
    val changeType: ChangeType
)
