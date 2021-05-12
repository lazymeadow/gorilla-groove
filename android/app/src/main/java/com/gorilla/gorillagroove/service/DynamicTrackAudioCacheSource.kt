package com.gorilla.gorillagroove.service

import android.annotation.SuppressLint
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import com.gorilla.gorillagroove.BuildConfig
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.service.GGLog.logCrit
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.GGLog.logWarn
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

/**
 * This piece of garbage will instruct Exoplayer to save audio to disk while we are streaming it. This means that, as long as the user has allocated space for it,
 * they can have music available for offline listening without explicitly marking songs as such.
 *
 * For a variety of reasons, we do not want to use Exoplayer's cache. Our own cache policy is complex, with different tracks having different eviction rules.
 * On top of that, we need to manage the eviction of album art alongside the track audio, which Exoplayer has no concept of.
 *
 * To achieve this, we need to tell Exoplayer to cache the audio. It will use its own cache. When exoplayer has completely finished caching a song, we then need
 * to copy the file out of Exoplayer's cache and into our own cache directory.
 *
 * Exoplayer's cache is set up to be dumped every time the app launches, and its size is restricted. Only the GG cache is "permanent".
 */
class DynamicTrackAudioCacheSource(val trackId: Long) : DataSource.Factory {

    private val trackDao get() = GorillaDatabase.trackDao

    override fun createDataSource(): DataSource {
        val fileDataSource = FileDataSource()

        fileDataSource.addTransferListener(object : TransferListener {
            override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
                val cacheKey = dataSpec.key ?: run {
                    logCrit("No dataSpec key found!")
                    return
                }

                val trackId = cacheKey.toLong()

                val track = trackDao.findById(trackId) ?: run {
                    logError("Could not load track $trackId after it was cached!")
                    return
                }

                // We use the CachingDataSource here for all plays, even ones that are already cached. So it happens often that we come in here with a cached song already.
                // No reason to assume our cache is bad and replace it (there are checks when we start playing the track). Just abort.
                if (track.songCachedAt != null) {
                    return
                }

                // Some more conditions where we don't want to cache things
                if (track.inReview || track.offlineAvailability == OfflineAvailabilityType.ONLINE_ONLY) {
                    return
                }

                logDebug("The cache transfer has finished of track: $cacheKey")

                val bytes = cache.getCachedBytes(cacheKey, 0, C.LENGTH_UNSET.toLong())

                if (bytes != track.filesizeAudio.toLong()) {
                    logDebug("Track $trackId finished cache transfer but not all bytes were present. Contained $bytes in the cache but track needs ${track.filesizeAudio} bytes to be cached.")
                    return
                }

                logDebug("Track $trackId finished cache transfer and all bytes are present. Copying data into GG Cache")

                val cacheDestination = TrackCacheService.getFileLocation(trackId, CacheType.AUDIO)

                if (cacheDestination.exists()) {
                    logWarn("Dynamically caching $trackId and the cache already exists. This is unexpected, and it will be overwritten.")
                }

                try {
                    val assembledCacheFile = getCacheFile(cacheKey)
                    if (assembledCacheFile.length() == track.filesizeAudio.toLong()) {
                        assembledCacheFile.copyTo(cacheDestination, overwrite = true)

                        // Safe to delete even during playback as this is a temporary file we made
                        // to assemble exoplayer's cache parts
                        assembledCacheFile.delete()
                    } else {
                        logCrit("Was about to move exoplayer cache file to GG cache, but its length (${assembledCacheFile.length()}) was not correct. Expected ${track.filesizeAudio}")
                        assembledCacheFile.delete()
                        return
                    }
                } catch (e: Throwable) {
                    logError("Could not copy exoplayer cached file to GG cache!", e)
                }

                track.songCachedAt = Instant.now()
                trackDao.save(track)

                logInfo("$trackId cache was moved to GG cache")
            }

            override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {}
            override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}
            override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}
        })

        return CacheDataSource(
            cache,
            defaultDataSource,
            fileDataSource,
            CacheDataSink(cache, C.LENGTH_UNSET.toLong()),
            CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
            null,
            CacheKeyProvider()
        )
    }

    fun getCacheFile(key: String): File {
        val path = "${GGApplication.application.cacheDir.absolutePath}/track-assembly-cache/"
        File(path).mkdirs()

        val tmpFile = File(path + key)
        if (tmpFile.exists()) {
            tmpFile.delete()
        }

        val cachePartialFiles = cache.getCachedSpans(key).sortedBy { it.position }

        try {
            FileOutputStream(tmpFile).use { outStream ->
                cachePartialFiles.forEach { cachePartialFile ->
                    outStream.write(cachePartialFile.file!!.readBytes())
                }
            }
        } catch (e: Throwable) {
            logError("Could not assemble cache spans into a full file!", e)
        }

        return tmpFile
    }

    companion object {
        private val cache = SimpleCache(
            File(GGApplication.application.cacheDir, "media"),
            // The biggest track on GG web right now is 69 (nice) MB. We clear this cache every time we
            // launch the app, so it's not that big of a deal to keep it small. Want to make sure it's big
            // enough for the largest track to be dynamically loaded though.
            LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100),
            ExoDatabaseProvider(GGApplication.application),
        )

        private val userAgent get() = Util.getUserAgent(GGApplication.application, BuildConfig.APPLICATION_ID)

        @SuppressLint("StaticFieldLeak") // Not a memory leak because we gave it the application context, which cannot be leaked, as there is only one and always one.
        val defaultDataSource = DefaultDataSourceFactory(
            GGApplication.application,
            DefaultHttpDataSourceFactory(userAgent)
        ).createDataSource()

        // We don't use the exoplayer cache for anything other than moving stuff into our own cache.
        // I have had weird things happen with their cache (like the cache reporting the correct file size, but the file not reporting the correct size),
        // and purging it seems to help make sure any issues aren't long-lasting.
        fun purgeCache() {
            cache.keys.forEach { key ->
                cache.removeResource(key)
            }
        }
    }
}

// Probably doesn't REALLY matter because we delete the exoplayer cache every app launch, but because our track URIs are dynamically
// generated from Amazon, the cache can't be re-used between track link generations without this. It is theoretically possible that somebody
// listens to a track and it's only partially cached. Then they move to a new track. Stop using the app. Then wait a few hours to start again.
// They go back and listen to the first track, and a different link will be generated and their prior cache is useless. With this key generation,
// they can re-use the partial cache they had built up prior. You know, assuming they didn't re-launch the app entirely between listens.
class CacheKeyProvider : CacheKeyFactory {
    @Suppress("UNCHECKED_CAST") // Nothing we can do. They require us to do an unchecked cast because it's not generic.
    override fun buildCacheKey(dataSpec: DataSpec): String {
        val customData = dataSpec.customData as Map<String, String>
        return customData.getValue("trackId")
    }
}
