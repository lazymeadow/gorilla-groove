package com.gorilla.gorillagroove.service

import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.service.GGLog.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.Instant

object TrackCacheService {
    private val trackDao get() = GorillaDatabase.trackDao

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

    fun getFileLocation(trackId: Long, cacheType: CacheType): File {
        val path = allPaths.getValue(cacheType) + "${trackId}.${cacheType.extension}"
        return File(path)
    }

    private fun getCacheItem(trackId: Long, cacheType: CacheType): File? {
        return getFileLocation(trackId, cacheType).takeIf { it.exists() }
    }

    suspend fun getCacheItemIfAvailable(trackId: Long, cacheType: CacheType): File? = withContext(Dispatchers.IO) {
        // Always want to look up the track ourselves to make sure the data we're getting isn't stale
        val track = trackDao.findById(trackId) ?: run {
            logError("No track with ID $trackId found when checking cache availability!")
            return@withContext null
        }

        when (cacheType) {
            CacheType.AUDIO -> if (track.songCachedAt == null) return@withContext null
            CacheType.ART -> if (track.artCachedAt == null) return@withContext null
            CacheType.THUMBNAIL -> if (track.thumbnailCachedAt == null) return@withContext null
        }

        getCacheItem(track.id, cacheType)?.let { cachedFile ->
            return@withContext cachedFile
        } ?: run {
            logError("Track ${track.id} was missing cacheType $cacheType! Marking track as not available offline")
            val refreshedTrack = trackDao.findById(track.id) ?: run {
                logError("Could not find refreshed track with ID: ${track.id}!")
                return@withContext null
            }

            when (cacheType) {
                CacheType.AUDIO -> track.songCachedAt = null
                CacheType.ART -> track.artCachedAt = null
                CacheType.THUMBNAIL -> track.thumbnailCachedAt = null
            }

            trackDao.save(refreshedTrack)
            return@withContext null
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun cacheTrack(trackId: Long, serverUrl: String, cacheType: CacheType): Long = withContext(Dispatchers.IO) {
        return@withContext try {
            val localPath = allPaths.getValue(cacheType) + "$trackId.${cacheType.extension}"
            URL(serverUrl).openStream().use { input ->
                FileOutputStream(File(localPath)).use { output -> input.copyTo(output) }
            }

            val track = trackDao.findById(trackId) ?: run {
                logError("Failed to find track with ID $trackId while saving $cacheType cache!")
                deleteCacheOnDisk(trackId, cacheType)
                return@withContext -1
            }

            val bytesChanged = when (cacheType) {
                CacheType.AUDIO -> {
                    track.songCachedAt = Instant.now()
                    track.filesizeAudio
                }
                CacheType.ART -> {
                    track.artCachedAt = Instant.now()
                    track.filesizeArt
                }
                CacheType.THUMBNAIL -> {
                    track.thumbnailCachedAt = Instant.now()
                    track.filesizeThumbnail
                }
            }

            trackDao.save(track)
            bytesChanged.toLong()
        } catch (e: Throwable) {
            logError("Track $trackId failed to download $cacheType from URL: $serverUrl", e)
            -1
        }
    }

    fun deleteCache(track: DbTrack, cacheTypes: Set<CacheType>): Long {
        cacheTypes.forEach { cacheType ->
            deleteCacheOnDisk(track.id, cacheType)
        }

        var bytesRemoved = 0L
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
