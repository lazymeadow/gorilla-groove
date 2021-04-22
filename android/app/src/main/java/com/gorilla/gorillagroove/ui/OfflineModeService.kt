package com.gorilla.gorillagroove.ui

import android.content.Context
import androidx.work.*
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.di.NetworkModule
import com.gorilla.gorillagroove.repository.logNetworkException
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.getNullableLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.Instant


object OfflineModeService {

    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()

    @Synchronized
    fun downloadAlwaysOfflineTracks() {
        val workManager = WorkManager.getInstance(GGApplication.application)

        val tracksNeedingCache = trackDao.getTracksNeedingCached()

        // TODO verify there's enough space to do the download

        val workRequests = tracksNeedingCache.map { track ->
            OneTimeWorkRequest.Builder(TrackDownloadWorker::class.java)
                .setInputData(workDataOf("trackId" to track.id))
                .build()
        }

        if (workRequests.isNotEmpty()) {
            workManager.enqueue(workRequests)
        }
    }
}

object TrackCacheService {
    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()

    private val trackCachePath: String by lazy { "${GGApplication.application.filesDir.absolutePath}/track-cache/" }
    val audioCachePath = trackCachePath + "audio/"
    val artCachePath = trackCachePath + "art/"
    val thumbnailCachePath = trackCachePath + "thumbnail/"

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

    fun getCacheItem(trackId: Long, cacheType: CacheType): File? {
        val localFilePath = allPaths.getValue(cacheType) + "${trackId}.${cacheType.extension}"
        return File(localFilePath).takeIf { it.exists() }
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
            return@withContext Result.failure()
        }

        TrackCacheService.cacheTrack(trackId, trackLinks.trackLink, CacheType.AUDIO)

        trackLinks.albumArtLink?.let { artLink ->
            TrackCacheService.cacheTrack(trackId, artLink, CacheType.ART)
        }

        logInfo("Finished download of '${OfflineAvailabilityType.AVAILABLE_OFFLINE}' track with ID: $trackId")

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
