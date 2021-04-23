package com.gorilla.gorillagroove.service.sync

import com.gorilla.gorillagroove.database.dao.TrackDao
import com.gorilla.gorillagroove.database.entity.DbSyncStatus
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.network.NetworkApi
import java.time.Instant

class TrackSynchronizer(
    private val networkApi: NetworkApi,
    trackDao: TrackDao,
) : StandardSynchronizer<DbTrack, TrackResponse>(trackDao) {
    override suspend fun fetchEntities(syncStatus: DbSyncStatus, maximum: Instant, page: Int): EntityChangeResponse<TrackResponse> {
        return networkApi.getTrackSyncEntities(
            minimum = syncStatus.lastSynced?.toEpochMilli() ?: 0,
            maximum = maximum.toEpochMilli(),
            page = page
        )
    }

    override suspend fun convertToDatabaseEntity(networkEntity: TrackResponse) = networkEntity.asTrack()

    // TODO use this to invalidate cache once we have a cache if the track or art are changed
    // Make sure to also purge tracks that are marked "NEVER" on caching
    override fun onEntityUpdate(entity: DbTrack) {
        super.onEntityUpdate(entity)
    }
}

data class TrackResponse(
    override val id: Long,
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
) : EntityResponse {
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
