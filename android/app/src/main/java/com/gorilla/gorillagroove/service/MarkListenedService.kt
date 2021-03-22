package com.gorilla.gorillagroove.service

import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.gorilla.gorillagroove.repository.MainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Upon listening to this much of a song, it is considered listened to
private const val TARGET_LISTEN_PERCENT = 0.60

class MarkListenedService(
    private val mainRepository: MainRepository,
    private val musicServiceConnection: MusicServiceConnection
) {

    private var currentSongMarkedListenedTo = false
    private var lastSongListenedMillis = 0L
    private var currentSongListenedAmount = 0L
    private var currentSongListenTarget = 0L

    init {
        musicServiceConnection.currentSongTimeMillis.observeForever { newTrackPosition ->
            handleMarkListened(newTrackPosition)
        }

        musicServiceConnection.nowPlaying.observeForever { newMetadataItem ->
            synchronized(this) {
                val trackLength = newMetadataItem.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

                currentSongMarkedListenedTo = false
                currentSongListenTarget = (trackLength * TARGET_LISTEN_PERCENT).toLong()
                lastSongListenedMillis = 0
                currentSongListenedAmount = 0
            }
        }
    }

    @Synchronized
    private fun handleMarkListened(nextPositionMillis: Long) {
        val trackId = musicServiceConnection.nowPlaying.value.description?.mediaId?.toLongOrNull() ?: run {
            Log.e("e", "Could not find track ID when marking song as listened to!")
            return
        }

        if (currentSongMarkedListenedTo) {
            return
        }

        val timeChange = nextPositionMillis - lastSongListenedMillis
        lastSongListenedMillis = nextPositionMillis

        if (timeChange < 0 || timeChange > 1500) {
            Log.i(
                "i",
                "Time change between last listen check was $timeChange ms, which was an abnormal amount. Assuming user skipped around the seek bar."
            )

            return
        }

        currentSongListenedAmount += timeChange

        if (currentSongListenedAmount > currentSongListenTarget) {
            currentSongMarkedListenedTo = true

            CoroutineScope(Dispatchers.IO).launch {
                mainRepository.markTrackListenedTo(trackId)
            }

            Log.i("i", "User finished listening to the song")
        } else {
            Log.i("i", "$currentSongListenedAmount, $currentSongListenTarget, $timeChange")
        }
    }

    companion object {
        @Volatile
        private var instance: MarkListenedService? = null

        fun getInstance(mainRepository: MainRepository, musicServiceConnection: MusicServiceConnection): MarkListenedService {
            return synchronized(this) {
                instance ?: MarkListenedService(mainRepository, musicServiceConnection)
                    .also { instance = it }
            }
        }
    }
}
