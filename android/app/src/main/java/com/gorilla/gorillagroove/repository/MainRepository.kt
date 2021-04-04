package com.gorilla.gorillagroove.repository

import android.content.SharedPreferences
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.network.*
import com.gorilla.gorillagroove.model.*
import com.gorilla.gorillagroove.network.login.LoginRequest
import com.gorilla.gorillagroove.network.track.TrackLinkResponse
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.util.Constants.KEY_SORT
import com.gorilla.gorillagroove.util.Constants.KEY_USER_TOKEN
import com.gorilla.gorillagroove.util.Constants.SORT_BY_AZ
import com.gorilla.gorillagroove.util.Constants.SORT_BY_DATE_ADDED_NEWEST
import com.gorilla.gorillagroove.util.Constants.SORT_BY_DATE_ADDED_OLDEST
import com.gorilla.gorillagroove.util.Constants.SORT_BY_ID
import com.gorilla.gorillagroove.util.DataState
import com.gorilla.gorillagroove.util.SessionState
import com.gorilla.gorillagroove.util.StateEvent
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import com.gorilla.gorillagroove.BuildConfig
import com.gorilla.gorillagroove.network.login.UpdateDeviceVersionRequest
import com.gorilla.gorillagroove.network.track.MarkListenedRequest
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.Constants.SORT_BY_ARTIST_AZ
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.LinkedHashMap


class MainRepository(
    private val networkApi: NetworkApi,
    private val sharedPreferences: SharedPreferences,
    private val dataSourceFactory: DefaultDataSourceFactory,
    private val okClient: OkHttpClient
) {
    lateinit var webSocket: WebSocket

    private var userToken: String = sharedPreferences.getString(KEY_USER_TOKEN, "") ?: ""

    private var trackSorting: Sort = sharedPreferences.getString(KEY_SORT, SORT_BY_ID)?.toSort() ?: Sort.ID

    private var lastVerifiedTrack: Long? = null
    private lateinit var lastFetchedLinks: TrackLinkResponse

    init {
        initWebSocket()
    }

    var dataSetChanged = false
    var currentIndex = 0

    val libraryConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val libraryMetadataList = mutableListOf<MediaMetadataCompat>()

    val playlistConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val playlistMetadataList = mutableListOf<MediaMetadataCompat>()

    //This is directly tied to Now Playing Fragment display
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

//    fun changeMediaSource(callingFragment: String, playlistId: Long?) {
//        when (callingFragment) {
//            CALLING_FRAGMENT_LIBRARY -> {
//                dataSetChanged = true
//                nowPlayingTracks.clear()
//                nowPlayingConcatenatingMediaSource.clear()
//                nowPlayingMetadataList.clear()
//
//                nowPlayingTracks.addAll(allTracks.values.toList())
//                nowPlayingTracks.sort(trackSorting)
//                nowPlayingTracks.map {
//                    nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
//                    nowPlayingMetadataList.add(it.toMediaMetadataItem())
//                }
//            }
//            CALLING_FRAGMENT_PLAYLIST -> {
//                dataSetChanged = true
//                nowPlayingTracks.clear()
//                nowPlayingConcatenatingMediaSource.clear()
//                nowPlayingMetadataList.clear()
//
//                val playlistItems = playlists.find { pId -> pId.id == playlistId }?.playlistItems
//
//                playlistItems?.map { it.track }?.let { nowPlayingTracks.addAll(it) }
//                nowPlayingTracks.map {
//                    nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
//                    nowPlayingMetadataList.add(it.toMediaMetadataItem())
//                }
//            }
//        }
//    }

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


    fun setSelectedTracks(trackIds: List<Long>, selectionOperation: SelectionOperation) {
        return
//        when (selectionOperation) {
//            SelectionOperation.PLAY_NOW -> {
//                dataSetChanged = true
//                nowPlayingTracks.clear()
//                nowPlayingConcatenatingMediaSource.clear()
//                nowPlayingMetadataList.clear()
//
//                trackIds.map { allTracks[it]?.let { track -> nowPlayingTracks.add(track) } }
//                nowPlayingTracks.map {
//                    nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
//                    nowPlayingMetadataList.add(it.toMediaMetadataItem())
//                }
//
//
//            }
//            SelectionOperation.PLAY_NEXT -> {
//                trackIds.asReversed().map {
//                    allTracks[it]?.let { track ->
//                        insertNowPlayingTrack(track)
//                    }
//                }
//            }
//            SelectionOperation.PLAY_LAST -> {
//                trackIds.asReversed().map {
//                    allTracks[it]?.let { track ->
//                        addToEndNowPlayingTrack(track)
//                    }
//                }
//            }
//        }

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

    private fun readyLibrarySources(tracks: List<DbTrack>) {
        libraryConcatenatingMediaSource.clear()
        libraryMetadataList.clear()
        tracks.map {
            libraryConcatenatingMediaSource.addCustomMediaSource(it)
            libraryMetadataList.add(it.toMediaMetadataItem())
        }
    }

    private fun readyPlaylistSources(tracks: List<DbTrack>) {
        playlistConcatenatingMediaSource.clear()
        playlistMetadataList.clear()
        tracks.map {
            playlistConcatenatingMediaSource.addCustomMediaSource(it)
            playlistMetadataList.add(it.toMediaMetadataItem())
        }
    }

    suspend fun getNowPlayingTracks(): Flow<DataState<out List<DbTrack>>> = flow {
        emit(DataState(nowPlayingTracks, StateEvent.Success))
    }

    // Needs to be rewritten with new entities in mind and maybe not being event-based
//    suspend fun updateTrack(trackUpdate: TrackUpdate): Flow<DataState<*>> = flow {
//        emit(DataState(null, StateEvent.Loading))
//        try {
//
//            networkApi.updateTrack(trackUpdate)
//            val updatedTrack = networkMapper.mapFromTrackEntity(
//                networkApi.getTrack(trackUpdate.trackIds[0])
//            )
//            val oldTrack = allTracks[updatedTrack.id]
//            allTracks[updatedTrack.id] = updatedTrack
//            allLibraryTracks[allLibraryTracks.indexOf(oldTrack)] = updatedTrack
//
//            databaseDao.updateTrack(cacheMapper.mapToTrackEntity(updatedTrack))
//
//            emit(DataState(null, StateEvent.Success))
//        } catch (e: Exception) {
//            emit(DataState(null, StateEvent.Error))
//        }
//    }

    suspend fun getToken(loginRequest: LoginRequest): Flow<SessionState<*>> = flow {
        emit(SessionState(null, StateEvent.Loading))
        try {
            val loginResponse = networkApi.login(loginRequest)
            userToken = loginResponse.token

            sharedPreferences.edit()
                .putString(KEY_USER_TOKEN, userToken)
                .apply()

            initWebSocket()

            emit(SessionState(loginResponse, StateEvent.AuthSuccess))
        } catch (e: Exception) {
            emit(SessionState(null, StateEvent.Error))
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

private fun String.toSort(): Sort {
    return when (this) {
        SORT_BY_ID -> Sort.ID
        SORT_BY_AZ -> Sort.A_TO_Z
        SORT_BY_DATE_ADDED_NEWEST -> Sort.NEWEST
        SORT_BY_DATE_ADDED_OLDEST -> Sort.OLDEST
        SORT_BY_ARTIST_AZ -> Sort.ARTIST_A_TO_Z
        else -> Sort.ID
    }
}


//enum class Sort(i: Int) {ID(5), A_TO_Z, NEWEST, OLDEST}
enum class Sort { ID, A_TO_Z, NEWEST, OLDEST, ARTIST_A_TO_Z }
enum class SelectionOperation { PLAY_NOW, PLAY_NEXT, PLAY_LAST }
