package com.gorilla.gorillagroove.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.sync.TrackResponse
import com.gorilla.gorillagroove.ui.ChangeType
import kotlinx.android.synthetic.main.review_queue_carousel_item.view.*
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

    // Three cases for "artistFilter"
    // 1) It's null. We want to get everything. The SQL handles this
    // 2) It's empty. We want to get tracks with NO artist explicitly.
    // 3) We have an actual search term. In this case, add % so that SQL will do a wildcard search as we want these to be inclusive
    fun getArtistSqlFilter(filter: String?): String? {
        return if (!filter.isNullOrEmpty()) { "%$filter%" } else filter
    }

    // Same story as the comment on "getArtistSqlFilter", but we use this for filtering live HTTP responses instead of doing it in SQL
    fun passesArtistFilter(filter: String?, track: TrackResponse): Boolean {
        return when {
            // If we have no filter, then get everything
            filter == null -> true
            // If we have a filter that is empty, there needs to be no artist
            filter.isEmpty() -> track.artist.isEmpty() && track.featuring.isEmpty()
            // Otherwise, do a contains so that we get anything where an artist was found since there can be multiple artists in the field
            else -> track.artist.contains(filter) || track.featuring.contains(filter)
        }
    }

    fun broadcastTrackChange(tracks: List<DbTrack>, changeType: ChangeType) {
        EventBus.getDefault().post(TrackChangeEvent(tracks, changeType))
    }

    // TODO I made this rather late. It would be nice to reuse this in the other places we get art.
    // Would need to incorporate a short-term in-memory cache for the AlbumArt view, and art resize for the notification one, I think. Unless that stuff just isn't actually needed.
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getAlbumArt(track: DbTrack): Bitmap? = withContext(Dispatchers.IO) {
        TrackCacheService.getCacheItemIfAvailable(track.id, CacheType.ART)?.let { cachedArtFile ->
            return@withContext BitmapFactory.decodeFile(cachedArtFile.absolutePath)
        }

        // We're DOING IT LIVE
        Network.api.getTrackLink(track.id, "LARGE").albumArtLink?.let { artLink ->
            return@withContext Glide.with(GGApplication.application)
                .asBitmap()
                .load(artLink)
                .submit()
                .get()
        }

        return@withContext null
    }
}

data class TrackChangeEvent(
    val tracks: List<DbTrack>,
    val changeType: ChangeType
)
