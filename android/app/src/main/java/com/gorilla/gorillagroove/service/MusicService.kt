package com.gorilla.gorillagroove.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import com.gorilla.gorillagroove.BuildConfig
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.GGLog.logCrit
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.GGLog.logWarn
import com.gorilla.gorillagroove.util.Constants.MEDIA_ROOT_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.time.Instant
import javax.inject.Inject


private const val TAG = "AppDebug: Music Service"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var repo: MainRepository

    // This is used. By simply having a reference, it gets constructed and does its own thing. Probably dumb. Idk how I'm supposed to start up a standalone singleton "service" in Dagger / Hilt
    @Suppress("unused")
    @Inject
    lateinit var markListenedService: MarkListenedService

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegroundService = false

    private val musicPlayerEventListener = PlayerEventListener()

    override fun onCreate() {
        super.onCreate()

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this),
            repo
        )

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(MusicPlaybackPreparer())
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        exoPlayer.addListener(musicPlayerEventListener)

        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

            when (playbackState) {
                Player.STATE_IDLE -> {
                    logDebug("Player state became IDLE")

                    musicNotificationManager.hideNotification()
                }
                Player.STATE_READY -> {
                    logDebug("Player state became READY")

                    musicNotificationManager.showNotification(exoPlayer)
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) {
                            stopForeground(false)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    logDebug("Player state became ENDED")

                    musicNotificationManager.hideNotification()
                }
                Player.STATE_BUFFERING -> {
                    logDebug("Player state became BUFFERING")
                }
                else -> {
                    logWarn("Player state became UNKNOWN ($playbackState)")

                    musicNotificationManager.hideNotification()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            logError("Player encountered an error", error)

            // If the exoplayer encounters an error, you have to re-prepare the entire thing. Pretty jank. It won't even try to play anything anymore unless you do so.
            // https://github.com/google/ExoPlayer/issues/4343
            preparePlayer(repo.nowPlayingConcatenatingMediaSource, repo.currentIndex, exoPlayer.currentPosition)
        }

        override fun onPositionDiscontinuity(reason: Int) {
            super.onPositionDiscontinuity(reason)

            when (reason) {
                Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> {
                    //Log.d(TAG, "onPositionDiscontinuity: next track window")
                }
                Player.DISCONTINUITY_REASON_SEEK -> {
                    //Log.d(TAG, "onPositionDiscontinuity: track seeked")
                }
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> {
                    //Log.d(TAG, "onPositionDiscontinuity: unable to seek to location")
                }
                Player.DISCONTINUITY_REASON_INTERNAL -> {
                    //Log.d(TAG, "onPositionDiscontinuity: reason internal")
                }
                Player.DISCONTINUITY_REASON_AD_INSERTION -> {
                }
            }
        }
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return repo.nowPlayingMetadataList[windowIndex].description
        }
    }

    private inner class MusicPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_URI or
                    PlaybackStateCompat.ACTION_PLAY_FROM_URI or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE

        override fun onPrepare(playWhenReady: Boolean) = Unit
        override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
            logDebug("Preparing media with ID: $mediaId")

            val itemToPlay = repo.nowPlayingMetadataList.find { it.id == mediaId }
            val songIndex = if (itemToPlay == null) 0 else repo.nowPlayingMetadataList.indexOf(itemToPlay)

            if (repo.dataSetChanged) {
                preparePlayer(repo.nowPlayingConcatenatingMediaSource, songIndex)
                repo.dataSetChanged = false
            } else {
                exoPlayer.seekTo(songIndex, 0)
                exoPlayer.playWhenReady = true
            }
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit
        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit
        override fun onCommand(
            player: Player,
            controlDispatcher: ControlDispatcher,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ) = false
    }

    private fun preparePlayer(
        concatSource: ConcatenatingMediaSource,
        songIndex: Int,
        currentPosition: Long = 0
    ) {
        logDebug("Preparing player")

        exoPlayer.stop(true)
        exoPlayer.setMediaSource(concatSource)
        exoPlayer.prepare() // triggers buffering
        exoPlayer.playWhenReady = true
        exoPlayer.seekTo(songIndex, currentPosition) //triggers seeked
        exoPlayer.setPlaybackParameters(PlaybackParameters(1f))
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        logInfo("Player task was removed. Stopping player")

        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()

        logWarn("Player is being destroyed")

        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                result.sendResult(null)
            }
        }
    }
}

class CacheDataSourceFactory(val trackId: Long) : DataSource.Factory {

    private val defaultDataSourceFactory: DefaultDataSourceFactory

    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()

    override fun createDataSource(): DataSource {
        val fileDataSource = FileDataSource()

        val cacheDataSource = CacheDataSource(
            cache,
            defaultDataSourceFactory.createDataSource(),
            fileDataSource,
            CacheDataSink(cache, C.LENGTH_UNSET.toLong()),
            CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
            null,
            CacheKeyProvider()
        )

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

                logDebug("The cache transfer has finished of track: $cacheKey")

                val bytes = cache.getCachedBytes(cacheKey, 0, C.LENGTH_UNSET.toLong())

                if (bytes == track.filesizeAudio.toLong()) {
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
                            logError("Was about to move exoplayer cache file to GG cache, but its length (${assembledCacheFile.length()}) was not correct. Expected ${track.filesizeAudio}")
                            assembledCacheFile.delete()
                            return
                        }
                    } catch (e: Throwable) {
                        logError("Could not copy exoplayer cached file to GG cache!", e)
                    }

                    val refreshedTrack = trackDao.findById(trackId) ?: run {
                        logError("Could not load refreshed Track when updating cache date!")
                        return
                    }

                    refreshedTrack.songCachedAt = Instant.now()
                    trackDao.save(refreshedTrack)

                    logInfo("$trackId cache was moved to GG cache")
                } else {
                    logDebug("Track $trackId finished cache transfer but not all bytes were present. Contained $bytes in the cache but track needs ${track.filesizeAudio} bytes to be cached.")
                }
            }

            override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {}
            override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}
            override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}
        })

        return cacheDataSource
    }

    init {
        val userAgent = Util.getUserAgent(GGApplication.application, BuildConfig.APPLICATION_ID)
        val bandwidthMeter = DefaultBandwidthMeter.Builder(GGApplication.application).build()
        defaultDataSourceFactory = DefaultDataSourceFactory(
            GGApplication.application,
            bandwidthMeter,
            DefaultHttpDataSourceFactory(userAgent, bandwidthMeter)
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
            LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100)
        )

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

private fun String.parseTrackId(): Long? {
    val expectedNetworkBeginning = "https://gorilla-tracks.s3.us-west-2.amazonaws.com/music/"

    return when {
        this.startsWith(expectedNetworkBeginning) -> {
            this.substring(expectedNetworkBeginning.length)
                .substringBefore(".ogg")
                .toLongOrNull()
        }
        this.startsWith("file:///") -> {
            this.substringAfter("/audio/")
                .substringBefore(".ogg")
                .toLongOrNull()
        }
        else -> null
    }
}

class CacheKeyProvider : CacheKeyFactory {
    override fun buildCacheKey(dataSpec: DataSpec): String {
        return dataSpec.uri.toString().parseTrackId().toString()
    }
}
