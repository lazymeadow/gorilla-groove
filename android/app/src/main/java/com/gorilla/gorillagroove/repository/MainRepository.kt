package com.gorilla.gorillagroove.repository

import android.content.SharedPreferences
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import com.gorilla.gorillagroove.BuildConfig
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.network.NetworkApi
import com.gorilla.gorillagroove.network.OkHttpWebSocket
import com.gorilla.gorillagroove.network.login.UpdateDeviceVersionRequest
import com.gorilla.gorillagroove.network.track.MarkListenedRequest
import com.gorilla.gorillagroove.network.track.TrackLinkResponse
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.Constants.KEY_USER_TOKEN
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.WebSocket
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class MainRepository(
    private val networkApi: NetworkApi,
    private val sharedPreferences: SharedPreferences,
    private val dataSourceFactory: DefaultDataSourceFactory,
    private val okClient: OkHttpClient
) {
    lateinit var webSocket: WebSocket

    private var userToken: String = sharedPreferences.getString(KEY_USER_TOKEN, "") ?: ""

    private var lastVerifiedTrack: Long? = null
    private lateinit var lastFetchedLinks: TrackLinkResponse

    init {
        initWebSocket()
    }

    var dataSetChanged = false
    var currentIndex = 0

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
        if (nowPlayingTracks.size > 0) {
            nowPlayingTracks.add(track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(track)
            nowPlayingMetadataList.add(track.toMediaMetadataItem())
        } else {
            dataSetChanged = true
            nowPlayingTracks.add(track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(track)
            nowPlayingMetadataList.add(track.toMediaMetadataItem())
        }
    }


    fun sendNowPlayingToServer(track: MediaDescriptionCompat) {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            put("trackId", track.mediaId)
            put("isPlaying", "true")
        }

        val mes = jsonObject.toString()
        //Log.d(TAG, "sendNowPlayingToServer: $mes")
        webSocket.send(mes)
    }

    fun sendStoppedPlayingToServer() {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            //put("trackId", track.mediaId)
            put("isPlaying", "false")
        }

        val mes = jsonObject.toString()
        //Log.d(TAG, "sendStoppedPlayingToServer: $mes")
        webSocket.send(mes)
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
            lastFetchedLinks = networkApi.getTrackLink(id)
            lastVerifiedTrack = id
            lastFetchedLinks
        } catch (e: Exception) {
            logNetworkException("Could not fetch track links!", e)
            TrackLinkResponse(" ", null)
        }
    }

    suspend fun updateTrack(trackUpdate: TrackUpdate): DbTrack? {
        return try {
            val updatedTrack = networkApi.updateTrack(trackUpdate).items.first()

            updatedTrack.asTrack()
        } catch (e: Exception) {
            logError("Failed to update track!")
            null
        }
    }

    private fun initWebSocket() {
        if (userToken != "") {
            val request = Request.Builder()
                .url("wss://gorillagroove.net/api/socket")
                .build()
            webSocket = okClient.newWebSocket(request, OkHttpWebSocket())
        }
    }

    fun cleanUpAndCloseConnections() {
        okClient.dispatcher.executorService.shutdown()
    }

    private fun ConcatenatingMediaSource.addCustomMediaSource(track: DbTrack, index: Int? = null) {
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(dataSourceFactory, object : ResolvingDataSource.Resolver {
            var oldUri: Uri? = null
            var newUri: Uri? = null

            override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                if ((dataSpec.uri == oldUri || dataSpec.uri == newUri) && newUri?.path?.isNotBlank() == true) {
                    newUri?.let { return dataSpec.buildUpon().setUri(it).build() }
                }

                oldUri = dataSpec.uri
                lateinit var fetchedUri: Uri
                lateinit var fetchedUris: TrackLinkResponse
                runBlocking {
                    fetchedUris =
                        getTrackLinks(Integer.parseInt(dataSpec.uri.toString()).toLong())
                }

                fetchedUri = Uri.parse(fetchedUris.trackLink)
                newUri = fetchedUri

                return dataSpec.buildUpon().setUri(fetchedUri).build()
            }
        })

        val progressiveMediaSource = ProgressiveMediaSource.Factory(resolvingDataSourceFactory)
        if (index != null) {
            this.addMediaSource(index, progressiveMediaSource.createMediaSource(track.toMediaItem()))
        } else {
            this.addMediaSource(progressiveMediaSource.createMediaSource(track.toMediaItem()))
        }
    }

    suspend fun markTrackListenedTo(trackId: Long) {
        val markListenedRequest = MarkListenedRequest(
            trackId = trackId,
            timeListenedAt = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            ianaTimezone = TimeZone.getDefault().id,
            latitude = null, // TODO gather location
            longitude = null
        )

        try {
            networkApi.markTrackListened(markListenedRequest)
        } catch (e: Throwable) {
            logNetworkException("Could not mark track as listened to!", e)
        }
    }

    suspend fun uploadCrashReport(zip: File) {
        val multipartFile = MultipartBody.Part.createFormData(
            "file",
            "crash-report.zip",
            zip.asRequestBody()
        )

        try {
            networkApi.uploadCrashReport(multipartFile)
        } catch (e: Throwable) {
            logNetworkException("Could not upload crash report!", e)
        }
    }

    suspend fun postDeviceVersion() {
        val lastPostedVersionKey = "LAST_POSTED_VERSION"

        val version = BuildConfig.VERSION_NAME
        val lastPostedVersion = sharedPreferences.getString(lastPostedVersionKey, null)

        if (version == lastPostedVersion) {
            return
        }

        logInfo("The API hasn't been told that we are running version $version. Our last posted value was $lastPostedVersion. Updating API")

        try {
            networkApi.updateDeviceVersion(UpdateDeviceVersionRequest(version))

            sharedPreferences.edit().putString(lastPostedVersionKey, version).apply()
            logInfo("Posted version $version to the API")
        } catch (e: Throwable) {
            logNetworkException("Could not update device version!", e)
        }
    }

    private fun logNetworkException(message: String, e: Throwable) {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "No http error provided"
            logError("message \n${errorBody}")
        } else {
            logError(message, e)
        }
    }
}

fun DbTrack.toMediaMetadataItem(): MediaMetadataCompat = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, id.toString())
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, length.toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, name)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, album)
        .build()

fun DbTrack.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(id.toString())
        .build()

enum class SelectionOperation { PLAY_NOW, PLAY_NEXT, PLAY_LAST }
