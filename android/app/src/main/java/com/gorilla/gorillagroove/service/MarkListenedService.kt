package com.gorilla.gorillagroove.service

import android.support.v4.media.MediaMetadataCompat
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.Instant

// Upon listening to this much of a song, it is considered listened to
private const val TARGET_LISTEN_PERCENT = 0.60

class MarkListenedService(
    private val mainRepository: MainRepository,
    musicServiceConnection: MusicServiceConnection
) {

    private val trackDao get() = GorillaDatabase.trackDao

    private var currentTrackMarkedListenedTo = false
    private var currentTrackMarkedStarted = false
    private var lastTrackListenedMillis = 0L
    private var currentTrackListenedAmount = 0L
    private var currentTrackListenTarget = 0L

    private var currentTrackId: Long? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            musicServiceConnection.currentSongTimeMillis.collect { newTrackPosition ->
                synchronized(this) {
                    if (currentTrackMarkedListenedTo && newTrackPosition < 1500) {
                        logDebug("Track is at position $newTrackPosition when it has already been listened to. Assuming it is starting over and resetting listened state.")

                        resetListenedState()
                    }
                }

                handleMarkListened(newTrackPosition)
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            musicServiceConnection.nowPlaying.collect { newMetadataItem ->
                synchronized(this) {
                    // Not sure why, but this metadata item seems to get updated without any user interaction or the song switching.
                    // We obviously don't want to reset the "mark listened" progress unless the song actually changes
                    // (or starts over, but we handle that situation in the currentSongTimeMillis observer instead)
                    val trackId = newMetadataItem.id?.toLongOrNull()
                    if (currentTrackId == trackId) {
                        return@synchronized
                    }

                    // Once again, not sure why, but it is possible for these messages to come through with a negative length. I assume they are being initialized
                    // elsewhere in multiple steps and we are seeing partial updates broadcast before they are ready. Ignore these.
                    val trackLength = newMetadataItem.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    if (trackLength < 0) {
                        return@synchronized
                    }

                    logDebug("Metadata item changed. Resetting current song listen state. Last ID was ${currentTrackId}. New ID is $trackId. Track length is $trackLength")

                    currentTrackId = trackId
                    currentTrackListenTarget = (trackLength * TARGET_LISTEN_PERCENT).toLong()

                    resetListenedState()
                }
            }
        }
    }

    private fun resetListenedState() {
        currentTrackMarkedListenedTo = false
        currentTrackMarkedStarted = false
        lastTrackListenedMillis = 0
        currentTrackListenedAmount = 0
    }

    @Synchronized
    private fun handleMarkListened(nextPositionMillis: Long) {
        val trackId = currentTrackId ?: return
        if (currentTrackMarkedListenedTo || lastTrackListenedMillis == nextPositionMillis || currentTrackId == null) {
            return
        }

        val timeChange = nextPositionMillis - lastTrackListenedMillis
        lastTrackListenedMillis = nextPositionMillis

        if (timeChange < 0 || timeChange > 1500) {
            logInfo("Time change between last listen check was $timeChange ms, which was an abnormal amount. Assuming user skipped around the seek bar.")
            return
        }

        currentTrackListenedAmount += timeChange

        if ((currentTrackListenedAmount > 5000 || currentTrackListenTarget < 5000) && !currentTrackMarkedStarted) {
            currentTrackMarkedStarted = true

            trackDao.findById(trackId)?.let { track ->
                logDebug("Marking $trackId as started on device")
                track.startedOnDevice = Instant.now()
                trackDao.save(track)
            } ?: run {
                logError("Track $trackId was not found when marking it as started!")
            }
        }

        if (currentTrackListenedAmount > currentTrackListenTarget) {
            currentTrackMarkedListenedTo = true

            CoroutineScope(Dispatchers.IO).launch {
                mainRepository.markTrackListenedTo(trackId)
            }

            logInfo("User finished listening to track $trackId")
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
