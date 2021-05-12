package com.gorilla.gorillagroove.service

import android.support.v4.media.MediaMetadataCompat
import com.google.gson.Gson
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.network.track.MarkListenedRequest
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.LocationService
import com.gorilla.gorillagroove.util.sharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// Upon listening to this much of a song, it is considered listened to. Doesn't matter if it was the start, middle, or end. Or the same part repeated over and over. 60% of total duration.
private const val TARGET_LISTEN_PERCENT = 0.60

class MarkListenedService(
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
                markListenedTo(trackId)
            }
        }
    }

    private suspend fun markListenedTo(trackId: Long) {
        logInfo("Marking track $trackId as listened to")

        val location = try {
            LocationService.getCurrentLocation()
        } catch (e: Throwable) {
            logError("Could not get location", e)
            null
        }

        val markListenedRequest = MarkListenedRequest(
            trackId = trackId,
            timeListenedAt = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            ianaTimezone = TimeZone.getDefault().id,
            latitude = location?.latitude,
            longitude = location?.longitude,
        )

        if (GGSettings.offlineModeEnabled) {
            logInfo("Offline mode enabled. Saving mark listened request for later")
            saveFailedTrackListen(markListenedRequest)
            return
        }

        sendListenedRequest(markListenedRequest)
    }

    companion object {
        @Volatile
        private var instance: MarkListenedService? = null

        fun getInstance(musicServiceConnection: MusicServiceConnection): MarkListenedService {
            return synchronized(this) {
                instance ?: MarkListenedService(musicServiceConnection)
                    .also { instance = it }
            }
        }

        @Synchronized
        private fun saveFailedTrackListen(markListenedRequest: MarkListenedRequest) {
            val existingRequestsList = getFailedListens()

            existingRequestsList.add(markListenedRequest)

            saveFailedListens(existingRequestsList)
        }

        @Suppress("UNCHECKED_CAST")
        @Synchronized
        private fun getFailedListens(): MutableList<MarkListenedRequest> {
            val gson = Gson()

            val existingRequestsJson = sharedPreferences.getString(FAILED_LISTENS_KEY, gson.toJson(emptyList<String>()))

            return gson.fromJson(existingRequestsJson, Array<MarkListenedRequest>::class.java).toMutableList()
        }

        @Synchronized
        private fun saveFailedListens(markListenedRequests: List<MarkListenedRequest>) {
            val json = Gson().toJson(markListenedRequests)

            sharedPreferences.edit().putString(FAILED_LISTENS_KEY, json).apply()
        }

        fun sendAndClearFailedListens() {
            if (GGSettings.offlineModeEnabled) {
                return
            }

            val listens = synchronized(this) {
                val failedListens = getFailedListens()

                // Clear out the existing failed listens. If our requests fail again, they'll be re-saved as normal
                saveFailedListens(emptyList())

                failedListens
            }

            if (listens.isEmpty()) {
                return
            }

            logInfo("Retrying failed listens: ${listens.map { it.trackId to it.timeListenedAt }}")

            GlobalScope.launch(Dispatchers.IO) {
                listens.forEach { sendListenedRequest(it) }
            }
        }

        private suspend fun sendListenedRequest(markListenedRequest: MarkListenedRequest) {
            try {
                Network.api.markTrackListened(markListenedRequest)
                logInfo("Track ${markListenedRequest.trackId} was marked listened to after the fact")
            } catch (e: Throwable) {
                logError("Could not mark track as listened to! Saving it to retry later", e)

                saveFailedTrackListen(markListenedRequest)
            }
        }

        private const val FAILED_LISTENS_KEY = "FAILED_LISTENS"
    }
}
