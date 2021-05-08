package com.gorilla.gorillagroove.ui

import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.EMPTY_PLAYBACK_STATE
import com.gorilla.gorillagroove.service.MusicServiceConnection
import com.gorilla.gorillagroove.util.KtLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlayerControlsViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    musicServiceConnection: MusicServiceConnection
) : ViewModel() {
    private val transportControls: MediaControllerCompat.TransportControls by lazy { musicServiceConnection.transportControls }

    private var updatePosition = true

    //Controls and Player Data
    private val _currentTrackItem: MutableLiveData<MediaMetadataCompat> = MutableLiveData()
    val currentTrackItem: LiveData<MediaMetadataCompat>
        get() = _currentTrackItem

    val playbackState = MutableStateFlow(EMPTY_PLAYBACK_STATE)

    private val _repeatState: MutableLiveData<Int> = MutableLiveData()
    val repeatState: LiveData<Int>
        get() = _repeatState

    private val _isBuffering: MutableLiveData<Boolean> = MutableLiveData()
    val isBuffering: LiveData<Boolean>
        get() = _isBuffering


    val mediaPosition = KtLiveData(0L)
    val bufferPosition = KtLiveData(0L)

    private var isPlaying = false
    private var currentMetadata: MediaMetadataCompat? = null
    private fun sendPlayStatusToServer() {
        if (isPlaying && currentMetadata != null) {
            currentMetadata?.description?.let { currTrack ->
                repository.sendNowPlayingToServer(currTrack)
            }
        } else if (!isPlaying && currentMetadata != null) {
            repository.sendStoppedPlayingToServer()
        } else {
            //probably initialization
        }

    }

    private val musicServiceConnection = musicServiceConnection.also { connection ->
        viewModelScope.launch(Dispatchers.Main) {
            launch {
                connection.playbackState.collect {
                    playbackState.value = it
                    when {
                        it.isPlaying -> {
                            isPlaying = true
                            sendPlayStatusToServer()
                        }
                        it.isPaused -> {
                            isPlaying = false
                            sendPlayStatusToServer()
                        }
                    }
                }
            }

            launch {
                connection.repeatState.collect { _repeatState.postValue(it) }
            }

            connection.nowPlaying.collect { newMetadataItem ->
                _currentTrackItem.postValue(newMetadataItem)

                if (newMetadataItem.description?.mediaId != "") {
                    if (currentMetadata?.description?.mediaId != newMetadataItem.description?.mediaId) {
                        currentMetadata = newMetadataItem
                        sendPlayStatusToServer()
                        newMetadataItem.description.mediaId?.toLongOrNull()?.let { newMediaId ->
                            val trackInNP = repository.nowPlayingTracks.find { track -> newMediaId == track.id }
                            val index = repository.nowPlayingTracks.indexOf(trackInNP)
                            repository.currentIndex = index

//                    ////Log.d(TAG, "current index: ${repository.currentIndex}")
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Main) {
            connection.currentSongTimeMillis.collect { newTime ->
                if (mediaPosition.value != newTime) {
                    mediaPosition.postValue(newTime)
                }
            }
        }
    }

    fun playMedia(startingIndex: Int, tracks: List<DbTrack>) {
        val startingTrack = tracks[startingIndex]

        repository.playTracks(tracks)

        transportControls.playFromMediaId(startingTrack.id.toString(), null)
    }

    fun playNow(track: DbTrack) {
//        repository.playTracks(listOf(track))

        transportControls.playFromMediaId(track.id.toString(), null)
    }

    fun playPause(): Boolean {
        return if (playbackState.value.isPaused) {
            transportControls.play()
            true
        } else {
            transportControls.pause()
            false
        }
    }

    fun pause() {
        transportControls.pause()
    }

    fun seekTo(position: Long) {
        transportControls.seekTo(position)
    }

    fun skipToNext() {
        transportControls.skipToNext()
    }

    fun skipToPrevious() {
        transportControls.skipToPrevious()
    }


    fun repeat() {
        when (_repeatState.value) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> {
                transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
            }
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
            }
            else -> {
                transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updatePosition = false
    }

    fun logout() {
        musicServiceConnection.transportControls.stop()
    }
}

inline val PlaybackStateCompat.isPaused
    get() = (state == PlaybackStateCompat.STATE_PAUSED) ||
            state == PlaybackStateCompat.STATE_STOPPED

inline val PlaybackStateCompat.isPlaying
    get() = (state == PlaybackStateCompat.STATE_BUFFERING) ||
            (state == PlaybackStateCompat.STATE_PLAYING)

inline val PlaybackStateCompat.currentPlayBackPosition: Long
    get() = if (state == PlaybackStateCompat.STATE_PLAYING) {
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (position + (timeDelta * playbackSpeed)).toLong()
    } else {
        position
    }
