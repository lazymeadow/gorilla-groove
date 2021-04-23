package com.gorilla.gorillagroove.ui

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.di.NetworkModule
import com.gorilla.gorillagroove.repository.logNetworkException
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.settings.GGSettings
import com.gorilla.gorillagroove.util.getNullableLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.Instant


// Because for some reason work manager doesn't let you know when stuff finished.
private val pendingTasks = mutableSetOf<Long>()

object OfflineModeService {

    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()

    @Synchronized
    suspend fun downloadAlwaysOfflineTracks() = withContext(Dispatchers.IO) {
        if (!GGSettings.offlineStorageEnabled) {
            return@withContext
        }

        logDebug("Downloading 'AVAILABLE_OFFLINE' tracks if needed")

        val workManager = WorkManager.getInstance(GGApplication.application)

        val tracksNeedingCache = trackDao.getTracksNeedingCached()
        val totalRequiredBytes = trackDao.getTotalBytesRequiredForFullCache()

        val allowedStorage = GGSettings.maximumOfflineStorageBytes

        var byteLimit = Long.MAX_VALUE
        if (allowedStorage < totalRequiredBytes) {
            // TODO alert user

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
        }
    }

    fun cleanUpCachedTracks() {
        logDebug("Checking if tracks need to be purged from cache")
        val allowedStorage = GGSettings.maximumOfflineStorageBytes

        val usedStorage = trackDao.getCachedTrackSizeBytes()

        // No need to clean anything up if we aren't over our cap
        if (allowedStorage > usedStorage) {
            logDebug("No track purge necessary")
            return
        }

        var bytesToPurge = usedStorage - allowedStorage

        logInfo("Cache is over-full! Need to purge $bytesToPurge bytes")

        // We are over our cap. First purge tracks that are not marked "AVAILABLE_OFFLINE"
        // They are ordered by least recency. So we can iterate and purge until we have purged enough.
        val dynamicCacheTracks = trackDao.getCachedTrackByOfflineTypeSortedByOldestStarted(OfflineAvailabilityType.NORMAL)
        dynamicCacheTracks.forEach { cachedTrack ->
            if (bytesToPurge > 0) {
                if (cachedTrack.songCachedAt != null) {
                    val bytesRemoved = TrackCacheService.deleteCache(cachedTrack, setOf(CacheType.AUDIO, CacheType.ART))
                    bytesToPurge -= bytesRemoved
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
                }
            }
        }

        logInfo("Cache has been reigned in")
    }
}

val DbTrack.bytesNeedingDownload: Int get() {
    val audioBytes = if (this.songCachedAt == null) this.filesizeAudio else 0
    val artBytes = if (this.artCachedAt == null) this.filesizeArt else 0

    return audioBytes + artBytes
}

object TrackCacheService {
    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()

    private val trackCachePath: String by lazy { "${GGApplication.application.filesDir.absolutePath}/track-cache/" }
    private val audioCachePath = trackCachePath + "audio/"
    private val artCachePath = trackCachePath + "art/"
    private val thumbnailCachePath = trackCachePath + "thumbnail/"

    private val allPaths = mapOf(
        CacheType.AUDIO to audioCachePath,
        CacheType.ART to artCachePath,
        CacheType.THUMBNAIL to thumbnailCachePath
    )

    init {
        allPaths.values.forEach { path -> File(path).mkdirs() }
    }

//    fun forceDeleteCache(trackId: Long) {
//        allPaths.forEach { pathBase ->
//            val path = pathBase
//        }
//    }

    private fun getCacheItem(trackId: Long, cacheType: CacheType): File? {
        val localFilePath = allPaths.getValue(cacheType) + "${trackId}.${cacheType.extension}"
        return File(localFilePath).takeIf { it.exists() }
    }

    fun getCacheItemIfAvailable(track: DbTrack, cacheType: CacheType): File? {
        when (cacheType) {
            CacheType.AUDIO -> if (track.songCachedAt == null) return null
            CacheType.ART -> if (track.artCachedAt == null) return null
            CacheType.THUMBNAIL -> if (track.thumbnailCachedAt == null) return null
        }

        getCacheItem(track.id, cacheType)?.let { cachedFile ->
            return cachedFile
        } ?: run {
            logError("Track ${track.id} was missing cacheType $cacheType! Marking track as not available offline")
            val refreshedTrack = trackDao.findById(track.id) ?: run {
                logError("Could not find refreshed track with ID: ${track.id}!")
                return null
            }

            when (cacheType) {
                CacheType.AUDIO -> track.songCachedAt = null
                CacheType.ART -> track.artCachedAt = null
                CacheType.THUMBNAIL -> track.thumbnailCachedAt = null
            }

            trackDao.save(refreshedTrack)
            return null
        }
    }

    fun cacheTrack(trackId: Long, serverUrl: String, cacheType: CacheType): Boolean {
        try {
            val localPath = allPaths.getValue(cacheType) + "$trackId.${cacheType.extension}"
            URL(serverUrl).openStream().use { input ->
                FileOutputStream(File(localPath)).use { output -> input.copyTo(output) }
            }

            val track = trackDao.findById(trackId) ?: run {
                logError("Failed to find track with ID $trackId while saving $cacheType cache!")
                deleteCacheOnDisk(trackId, cacheType)
                return false
            }

            when (cacheType) {
                CacheType.AUDIO -> track.songCachedAt = Instant.now()
                CacheType.ART -> track.artCachedAt = Instant.now()
                CacheType.THUMBNAIL -> track.thumbnailCachedAt = Instant.now()
            }

            trackDao.save(track)
        } catch (e: Throwable) {
            logError("Track $trackId failed to download $cacheType from URL: $serverUrl", e)
            return false
        }
        return true
    }

    fun deleteCache(track: DbTrack, cacheTypes: Set<CacheType>): Int {
        cacheTypes.forEach { cacheType ->
            deleteCacheOnDisk(track.id, cacheType)
        }

        var bytesRemoved = 0
        if (track.songCachedAt != null) {
            track.songCachedAt = null
            bytesRemoved += track.filesizeAudio
        }
        if (track.artCachedAt != null) {
            track.artCachedAt = null
            bytesRemoved += track.filesizeArt
        }

        trackDao.save(track)

        return bytesRemoved
    }

    fun deleteCacheOnDisk(trackId: Long, cacheType: CacheType) {
        val localPath = allPaths.getValue(cacheType) + "$trackId.${cacheType.extension}"

        val file = File(localPath)
        if (file.exists()) {
            file.delete()
        }
    }
}

enum class CacheType(val extension: String) {
    AUDIO("ogg"),
    ART("png"),
    THUMBNAIL("png")
}

@Suppress("BlockingMethodInNonBlockingContext")
class TrackDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

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

        TrackCacheService.cacheTrack(trackId, trackLinks.trackLink, CacheType.AUDIO)

        trackLinks.albumArtLink?.let { artLink ->
            TrackCacheService.cacheTrack(trackId, artLink, CacheType.ART)
        }

        logInfo("Finished download of '${OfflineAvailabilityType.AVAILABLE_OFFLINE}' track with ID: $trackId")

        pendingTasks.remove(trackId)
        if (pendingTasks.isEmpty()) {
            OfflineModeService.cleanUpCachedTracks()
        }
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
