package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.dao.TrackDao
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.network.NetworkApi
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import java.time.Instant

class TrackSynchronizer(
    private val networkApi: NetworkApi,
    private val trackDao: TrackDao,
) {

    suspend fun sync(
        syncStatus: DbSyncStatus,
        maximum: Instant
    ): Boolean {
        var currentPage = 0
        var pagesToGet: Int

        do {
            val (pageToFetch, success) = savePageOfChanges(syncStatus, maximum, currentPage)
            if (!success) {
                return false
            }

            pagesToGet = pageToFetch
            currentPage++
        } while (currentPage < pagesToGet)

        return true
    }

    private suspend fun savePageOfChanges(syncStatus: DbSyncStatus, maximum: Instant, page: Int): Pair<Int, Boolean> {
        logDebug("Syncing $syncStatus page $page")
        val response = try {
            networkApi.getTrackSyncEntities(
                minimum = syncStatus.lastSynced?.toEpochMilli() ?: 0,
                maximum = maximum.toEpochMilli(),
                page = page
            )
        } catch (e: Throwable) {
            logError("Could not sync page $page of tracks!", e)
            return -1 to false
        }

        val pagesToGet = response.pageable.totalPages

        val content = response.content

        content.new.forEach { trackResponse ->
            logDebug("Saving new track with ID: ${trackResponse.id}")
            val track = trackResponse.asTrack()

            trackDao.save(track)
        }

        content.modified.forEach { trackResponse ->
            logDebug("Saving updated track with ID: ${trackResponse.id}")
            val track = trackResponse.asTrack()

            trackDao.save(track)

            // TODO need to invalidate cache once caching is a thing
        }

        content.removed.forEach { trackId ->
            logDebug("Deleting track with ID: $trackId")

            trackDao.delete(trackId)
        }

        return pagesToGet to true
    }
}

data class TrackResponse(
    val id: Long,
    val name: String,
    val artist: String,
    val featuring: String,
    val album: String,
    val trackNumber: Int?,
    val length: Int,
    val releaseYear: Int?,
    val genre: String?,
    val playCount: Int,
    val private: Boolean,
    val inReview: Boolean,
    val hidden: Boolean,
    val lastPlayed: Instant?,
    val addedToLibrary: Instant?,
    val note: String?,
    val songUpdatedAt: Instant?,
    val artUpdatedAt: Instant?,
    val offlineAvailability: OfflineAvailabilityType,
    val filesizeSongOgg: Int,
    val filesizeSongMp3: Int,
    val filesizeArtPng: Int,
    val filesizeThumbnail64x64Png: Int,
    val reviewSourceId: Long?,
    val lastReviewed: Instant?,
) {
    fun asTrack() = DbTrack(
        id = id,
        name = name,
        artist = artist,
        featuring = featuring,
        album = album,
        trackNumber = trackNumber,
        length = length,
        releaseYear = releaseYear,
        genre = genre,
        playCount = playCount,
        thePrivate = private,
        inReview = inReview,
        hidden = hidden,
        lastPlayed = lastPlayed,
        addedToLibrary = addedToLibrary,
        note = note,
        offlineAvailability = offlineAvailability,
        filesizeAudio = filesizeSongOgg,
        filesizeArt = filesizeArtPng,
        filesizeThumbnail = filesizeThumbnail64x64Png,
        reviewSourceId = reviewSourceId,
        lastReviewed = lastReviewed
    )
}
