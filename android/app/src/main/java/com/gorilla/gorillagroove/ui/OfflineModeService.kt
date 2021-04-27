package com.gorilla.gorillagroove.ui

import android.content.Context
import androidx.work.*
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.di.NetworkModule
import com.gorilla.gorillagroove.repository.logNetworkException
import com.gorilla.gorillagroove.service.CacheType
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.TrackCacheService
import com.gorilla.gorillagroove.ui.settings.GGSettings
import com.gorilla.gorillagroove.ui.settings.toReadableByteString
import com.gorilla.gorillagroove.util.ShowAlertDialogRequest
import com.gorilla.gorillagroove.util.getNullableLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus


// Because for some reason work manager doesn't let you know when stuff finished.
private val pendingTasks = mutableSetOf<Long>()

object OfflineModeService {

    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()

    @Synchronized
    suspend fun downloadAlwaysOfflineTracks() = withContext(Dispatchers.IO) {
        if (!GGSettings.offlineStorageEnabled) {
            cleanUpCachedTracks()
            return@withContext
        }

        logDebug("Downloading 'AVAILABLE_OFFLINE' tracks if needed")

        val workManager = WorkManager.getInstance(GGApplication.application)

        val tracksNeedingCache = trackDao.getTracksNeedingCached()
        val totalRequiredBytes = trackDao.getTotalBytesRequiredForFullCache()

        val allowedStorage = GGSettings.maximumOfflineStorageBytes.value

        var byteLimit = Long.MAX_VALUE
        if (allowedStorage < totalRequiredBytes) {
            showStorageWarning(allowedStorage, totalRequiredBytes)

            // If we don't have enough room to download everything, figure out how many bytes we've used on ALWAYS_AVAILABLE music and fill in the gaps
            val existingAlwaysAvailableBytes = trackDao.getCachedTrackSizeBytes(OfflineAvailabilityType.AVAILABLE_OFFLINE)
            byteLimit = allowedStorage - existingAlwaysAvailableBytes
        }

        val workRequests = tracksNeedingCache
            // If we don't have enough storage space to download all AVAILABLE_OFFLINE music, only take them until we run out of space
            .filter { track ->
                val canDownload = track.bytesNeedingDownload < byteLimit
                if (canDownload) {
                    byteLimit -= track.bytesNeedingDownload
                }
                canDownload
            }
            .map { track ->
                pendingTasks.add(track.id)

                OneTimeWorkRequest.Builder(TrackDownloadWorker::class.java)
                    .setInputData(workDataOf("trackId" to track.id))
                    .build()
            }

        if (workRequests.isNotEmpty()) {
            logDebug("Enqueueing ${workRequests.size} download requests for 'AVAILABLE_OFFLINE' tracks")
            workManager.enqueue(workRequests)
        } else {
            cleanUpCachedTracks()
        }
    }

    var alertDialogShownThisSession = false // Just so there is no spam potential if they're tweaking storage. Not saved anywhere outside of memory
    private fun showStorageWarning(allowedStorage: Long, totalRequiredBytes: Long) {
        if (GGSettings.storageWarningSeen || alertDialogShownThisSession) {
            return
        }

        GGSettings.storageWarningSeen = true
        alertDialogShownThisSession = true

        // We have no reference to the current activity, so instead, broadcast an event that we want to show a dialog
        val dialogEvent = ShowAlertDialogRequest(
            title = "Insufficient Storage Configured",
            message = "You have ${allowedStorage.toReadableByteString()} storage allocated for offline music, but have ${totalRequiredBytes.toReadableByteString()} of music marked 'Available Offline'. " +
                    "Not all of your music will be able to be downloaded.\n\nYou can change your storage from Settings, or tap 'Quick fix' to increase your storage limit to {${totalRequiredBytes.toReadableByteString()}}",
            yesText = "Quick fix",
            noText = "Do nothing",
            yesAction = {
                logInfo("User opted to quick fix their storage problem")
                GGSettings.maximumOfflineStorageBytes.value = totalRequiredBytes
            },
            noAction = {
                logInfo("User ignored cache limit being exceeded")
            }
        )

        EventBus.getDefault().post(dialogEvent)
    }

    fun cleanUpCachedTracks() {
        logDebug("Checking if tracks need to be purged from cache")
        val allowedStorage = GGSettings.maximumOfflineStorageBytes.value

        val usedStorage = trackDao.getCachedTrackSizeBytes()

        // No need to clean anything up if we aren't over our cap
        if (allowedStorage > usedStorage) {
            logDebug("No track purge necessary")
            return
        }

        var bytesToPurge = usedStorage - allowedStorage

        logInfo("Cache is over-full! Need to purge ${bytesToPurge.toReadableByteString()}")

        // We are over our cap. First purge tracks that are not marked "AVAILABLE_OFFLINE"
        // They are ordered by least recency. So we can iterate and purge until we have purged enough.
        val dynamicCacheTracks = trackDao.getCachedTrackByOfflineTypeSortedByOldestStarted(OfflineAvailabilityType.NORMAL)
        dynamicCacheTracks.forEach { cachedTrack ->
            if (bytesToPurge > 0) {
                if (cachedTrack.songCachedAt != null) {
                    val bytesRemoved = TrackCacheService.deleteCache(cachedTrack, setOf(CacheType.AUDIO, CacheType.ART))
                    bytesToPurge -= bytesRemoved

                    val event = TrackCacheEvent(-bytesRemoved, CacheChangeType.DELETED, OfflineAvailabilityType.AVAILABLE_OFFLINE)
                    EventBus.getDefault().post(event)
                }
            }
        }

        if (bytesToPurge < 0) {
            logInfo("Cache has been reigned in")
            return
        }

        // We are now in a situation where the user has not given us enough storage to store everything marked "AVAILABLE_OFFLINE".
        // We cannot honor these tracks and the storage setting, and must purge the tracks.
        val alwaysOfflineCacheTracks = trackDao.getCachedTrackByOfflineTypeSortedByOldestStarted(OfflineAvailabilityType.AVAILABLE_OFFLINE)
        alwaysOfflineCacheTracks.forEach { cachedTrack ->
            if (bytesToPurge > 0) {
                if (cachedTrack.songCachedAt != null) {
                    val bytesRemoved = TrackCacheService.deleteCache(cachedTrack, setOf(CacheType.AUDIO, CacheType.ART))
                    bytesToPurge -= bytesRemoved

                    val event = TrackCacheEvent(-bytesRemoved, CacheChangeType.DELETED, OfflineAvailabilityType.AVAILABLE_OFFLINE)
                    EventBus.getDefault().post(event)
                }
            }
        }

        logInfo("Cache has been reigned in")
    }
}

private val DbTrack.bytesNeedingDownload: Int get() {
    val audioBytes = if (this.songCachedAt == null) this.filesizeAudio else 0
    val artBytes = if (this.artCachedAt == null) this.filesizeArt else 0

    return audioBytes + artBytes
}

@Suppress("BlockingMethodInNonBlockingContext")
class TrackDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()

    val networkApi = NetworkModule.provideTrackService(
        NetworkModule.provideRetrofit(
            NetworkModule.provideGsonBuilder()
        )
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val trackId = inputData.getNullableLong("trackId")
            ?: throw IllegalArgumentException("No trackId found when starting TrackDownloadWorker! InputData was: $inputData")

        logInfo("Starting download of '${OfflineAvailabilityType.AVAILABLE_OFFLINE}' track with ID: $trackId")

        val trackLinks = try {
            networkApi.getTrackLink(trackId)
        } catch (e: Throwable) {
            e.logNetworkException("Failed to get track links for offline download!")
            pendingTasks.remove(trackId)
            if (pendingTasks.isEmpty()) {
                OfflineModeService.cleanUpCachedTracks()
            }
            return@withContext Result.failure()
        }

        val track = trackDao.findById(trackId) ?: run {
            logError("Could not load track with ID $trackId while processing offline download!")
            return@withContext Result.failure()
        }

        val changeType = if (track.songCachedAt == null) CacheChangeType.ADDED else CacheChangeType.UPDATED
        var bytesChanged = TrackCacheService.cacheTrack(trackId, trackLinks.trackLink, CacheType.AUDIO)

        trackLinks.albumArtLink?.let { artLink ->
            bytesChanged += TrackCacheService.cacheTrack(trackId, artLink, CacheType.ART)
        }

        logInfo("Finished download of '${OfflineAvailabilityType.AVAILABLE_OFFLINE}' track with ID: $trackId")

        pendingTasks.remove(trackId)
        if (pendingTasks.isEmpty()) {
            OfflineModeService.cleanUpCachedTracks()
        }

        val event = TrackCacheEvent(bytesChanged, changeType, OfflineAvailabilityType.AVAILABLE_OFFLINE)
        EventBus.getDefault().post(event)

        return@withContext Result.success()
    }
}

private fun workDataOf(vararg data: Pair<String, Long>): Data {
    val builder = Data.Builder()

    data.forEach { (key, value) ->
        builder.putLong(key, value)
    }

    return builder.build()
}

data class TrackCacheEvent(
    val bytesChanged: Long,
    val cacheChangeType: CacheChangeType,
    val offlineAvailabilityType: OfflineAvailabilityType
)

enum class CacheChangeType {
    DELETED, UPDATED, ADDED
}
