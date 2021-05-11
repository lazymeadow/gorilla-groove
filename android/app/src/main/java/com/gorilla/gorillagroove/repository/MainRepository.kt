package com.gorilla.gorillagroove.repository

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import com.gorilla.gorillagroove.BuildConfig
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.network.OkHttpWebSocket
import com.gorilla.gorillagroove.network.login.UpdateDeviceVersionRequest
import com.gorilla.gorillagroove.network.track.TrackLinkResponse
import com.gorilla.gorillagroove.network.track.TrackUpdateRequest
import com.gorilla.gorillagroove.service.CacheType
import com.gorilla.gorillagroove.service.DynamicTrackAudioCacheSource
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.GGSettings
import com.gorilla.gorillagroove.service.TrackCacheService
import com.gorilla.gorillagroove.util.sharedPreferences
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONObject
import java.util.*

const val LAST_POSTED_VERSION_KEY = "LAST_POSTED_VERSION"

class MainRepository(
    private val okClient: OkHttpClient,
) {
    private var webSocket: WebSocket? = null

    private var lastVerifiedTrack: Long? = null
    private lateinit var lastFetchedLinks: TrackLinkResponse

    init {
        initWebSocket()
    }

    var dataSetChanged = false
    var currentIndex = 0

    val currentTrack: DbTrack?
        get() = nowPlayingTracks.getOrNull(currentIndex)

    val nowPlayingTracks = mutableListOf<DbTrack>()
    val nowPlayingConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val nowPlayingMetadataList = mutableListOf<MediaMetadataCompat>()

    fun playTracks(tracks: List<DbTrack>) {
        dataSetChanged = true

        nowPlayingTracks.clear()
        nowPlayingConcatenatingMediaSource.clear()
        nowPlayingMetadataList.clear()

        nowPlayingTracks.addAll(tracks)
        nowPlayingTracks.map {
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
            nowPlayingMetadataList.add(it.toMediaMetadataItem())
        }
    }

    private fun insertNowPlayingTrack(track: DbTrack) {
        if (nowPlayingTracks.size > 0) {
            nowPlayingTracks.add(currentIndex + 1, track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(track, currentIndex + 1)
            nowPlayingMetadataList.add(currentIndex + 1, track.toMediaMetadataItem())
        } else {
            dataSetChanged = true
            nowPlayingTracks.add(currentIndex, track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(track, currentIndex)
            nowPlayingMetadataList.add(currentIndex, track.toMediaMetadataItem())
        }
    }

    private fun addToEndNowPlayingTrack(track: DbTrack) {
        if (nowPlayingTracks.isEmpty()) {
            dataSetChanged = true
        }

        nowPlayingTracks.add(track)
        nowPlayingConcatenatingMediaSource.addCustomMediaSource(track)
        nowPlayingMetadataList.add(track.toMediaMetadataItem())
    }

    fun sendNowPlayingToServer(track: MediaDescriptionCompat) {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            put("trackId", track.mediaId)
            put("isPlaying", "true")
        }

        val mes = jsonObject.toString()
        if (webSocket == null) {
            initWebSocket()
        }
        webSocket?.send(mes)
    }

    fun sendStoppedPlayingToServer() {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            //put("trackId", track.mediaId)
            put("isPlaying", "false")
        }

        val mes = jsonObject.toString()
        webSocket?.send(mes)
    }

    fun setSelectedTracks(tracks: List<DbTrack>, selectionOperation: SelectionOperation) {
        when (selectionOperation) {
            SelectionOperation.PLAY_NOW -> {
                dataSetChanged = true
                nowPlayingTracks.clear()
                nowPlayingConcatenatingMediaSource.clear()
                nowPlayingMetadataList.clear()

                nowPlayingTracks.addAll(tracks)
                nowPlayingTracks.map {
                    nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
                    nowPlayingMetadataList.add(it.toMediaMetadataItem())
                }


            }
            SelectionOperation.PLAY_NEXT -> {
                tracks.forEach { track ->
                    insertNowPlayingTrack(track)
                }
            }
            SelectionOperation.PLAY_LAST -> {
                tracks.forEach { track ->
                    addToEndNowPlayingTrack(track)
                }
            }
        }
    }

    suspend fun getTrackLinks(id: Long): TrackLinkResponse {
        if (lastVerifiedTrack == id) {
            return lastFetchedLinks
        }

        return try {
            lastFetchedLinks = Network.api.getTrackLink(id)
            lastVerifiedTrack = id
            lastFetchedLinks
        } catch (e: Exception) {
            if (e !is CancellationException) {
                logError("Could not fetch track links!", e)
            }
            TrackLinkResponse(" ", null)
        }
    }

    suspend fun updateTrack(trackUpdate: TrackUpdateRequest): DbTrack? {
        return try {
            val updatedTrack = Network.api.updateTrack(trackUpdate).items.first()

            updatedTrack.asTrack()
        } catch (e: Exception) {
            logError("Failed to update track!")
            null
        }
    }

    private fun initWebSocket() {
        if (GGApplication.isUserSignedIn && !GGSettings.offlineModeEnabled) {
            logDebug("Initializing websocket")
            val request = Request.Builder()
                .url("wss://gorillagroove.net/api/socket")
                .build()
            webSocket = okClient.newWebSocket(request, OkHttpWebSocket())
        }
    }

    private fun ConcatenatingMediaSource.addCustomMediaSource(track: DbTrack, index: Int? = null) {
        val cacheDataSource = DynamicTrackAudioCacheSource(track.id)

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(cacheDataSource, object : ResolvingDataSource.Resolver {
            var oldUri: Uri? = null
            var newUri: Uri? = null

            override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                val customData = mapOf("trackId" to track.id.toString())
                if ((dataSpec.uri == oldUri || dataSpec.uri == newUri) && newUri?.path?.isNotBlank() == true) {
                    newUri?.let { return dataSpec.buildUpon().setCustomData(customData).setUri(it).build() }
                }

                oldUri = dataSpec.uri

                val specBuilder = dataSpec.buildUpon().setCustomData(customData)

                // The reference to the track could be old. Especially with caching, we want to make sure we have an up-to-date reference for deciding where to find our media files
                val refreshedTrack = GorillaDatabase.trackDao.findById(track.id) ?: run {
                    logError("Track ${track.id} no longer existed when refreshing it for media source playback!")
                    return dataSpec
                }

                val newSpec = runBlocking {
                    TrackCacheService.getCacheItemIfAvailable(refreshedTrack.id, CacheType.AUDIO)?.let { cachedAudioFile ->
                        logDebug("Loading cached track data")
                        if (cachedAudioFile.length() != refreshedTrack.filesizeAudio.toLong()) {
                            logError("The cached audio file had a length of ${cachedAudioFile.length()} but the expected audio size was ${refreshedTrack.filesizeAudio}! Assuming this file has been corrupted and deleting it.")
                            TrackCacheService.deleteCache(refreshedTrack, setOf(CacheType.AUDIO))
                            return@runBlocking null
                        }

                        val fileUri = Uri.fromFile(cachedAudioFile)
                        newUri = fileUri
                        return@runBlocking specBuilder.setUri(fileUri).build()
                    }
                }

                if (newSpec != null) {
                    return newSpec
                }

                // If we got here it means our cache didn't exist or had issues
                val fetchedUri = runBlocking {
                    logDebug("Fetching track links for track ${track.id}")
                    val fetchedUris = getTrackLinks(Integer.parseInt(dataSpec.uri.toString()).toLong())

                    if (
                        refreshedTrack.offlineAvailability != OfflineAvailabilityType.ONLINE_ONLY
                        && !refreshedTrack.inReview
                        && refreshedTrack.artCachedAt == null
                        && fetchedUris.albumArtLink != null
                    ) {
                        logDebug("Art was not cached for track ${track.id} but could be. Saving it for later")
                        GlobalScope.launch(Dispatchers.IO) {
                            TrackCacheService.cacheTrack(track.id, fetchedUris.albumArtLink, CacheType.ART)

                            logDebug("Art was cached for track ${track.id}")
                        }
                    }

                    return@runBlocking Uri.parse(fetchedUris.trackLink)
                }

                newUri = fetchedUri
                logDebug("Listening to track with uri: '$newUri'")

                return specBuilder.setUri(fetchedUri).build()
            }
        })

        val progressiveMediaSource = ProgressiveMediaSource.Factory(resolvingDataSourceFactory)
        if (index != null) {
            this.addMediaSource(index, progressiveMediaSource.createMediaSource(track.toMediaItem()))
        } else {
            this.addMediaSource(progressiveMediaSource.createMediaSource(track.toMediaItem()))
        }
    }

    suspend fun postDeviceVersion() {
        val version = BuildConfig.VERSION_NAME
        val lastPostedVersion = sharedPreferences.getString(LAST_POSTED_VERSION_KEY, null)

        if (version == lastPostedVersion || GGSettings.offlineModeEnabled) {
            return
        }

        logInfo("The API hasn't been told that we are running version $version. Our last posted value was $lastPostedVersion. Updating API")

        try {
            Network.api.updateDeviceVersion(UpdateDeviceVersionRequest(version))

            sharedPreferences.edit().putString(LAST_POSTED_VERSION_KEY, version).apply()
            logInfo("Posted version $version to the API")
        } catch (e: Throwable) {
            logError("Could not update device version!", e)
        }
    }
}

fun DbTrack.toMediaMetadataItem(): MediaMetadataCompat = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistString)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, id.toString())
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, length.toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, name)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artistString)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, album)
        .build()

fun DbTrack.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(id.toString())
        .build()

enum class SelectionOperation { PLAY_NOW, PLAY_NEXT, PLAY_LAST }
