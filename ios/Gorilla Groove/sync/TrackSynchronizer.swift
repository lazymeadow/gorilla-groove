import Foundation

class TrackSynchronizer {

    private static weak var reviewQueueController: ReviewQueueController? = nil
    
    static func sync(
        _ syncStatus: SyncStatus,
        _ maximum: Int,
        _ pageCompleteCallback: PageCompleteCallback?
    ) -> Bool {
        let url = String(format: ServerSynchronizer.baseUrl, syncStatus.syncType.rawValue, syncStatus.lastSynced.toEpochTime(), maximum)
        
        var currentPage = 0
        var pagesToGet = 0
        var success = true
        repeat {
            (pagesToGet, success) = savePageOfTrackChanges(url: url, page: currentPage)
            if !success {
                GGSyncLog.error("Could not sync Track changes!")
                return false
            }
            pageCompleteCallback?(currentPage, pagesToGet, syncStatus.syncType)
            currentPage += 1
        } while (currentPage < pagesToGet)
        
        GGSyncLog.info("Finished syncing tracks")
        return true
    }
    
    static private func savePageOfTrackChanges(url: String, page: Int) -> (Int, Bool) {
        var pagesToFetch = -1
        let (entityResponse, status, _) = HttpRequester.getSync(
            url + String(page),
            EntityChangeResponse<TrackResponse>.self
        )
        
        if (status < 200 || status >= 300 || entityResponse == nil) {
            GGSyncLog.error("Failed to sync new data!")
            
            return (-1, false)
        }
        
        guard let content = entityResponse?.content else { return (-1, false) }
        
        var newInReviewTracks: Array<Track> = []
        var modifiedInReviewTracks: Array<Track> = []
        var deletedInReviewTracks: Array<Track> = []

        for newTrackResponse in content.new {
            let newTrack = newTrackResponse.asTrack()
            TrackDao.save(newTrack)
            
            if newTrack.inReview {
                newInReviewTracks.append(newTrack)
            }
            
            GGSyncLog.debug("Added new track with ID: \(newTrackResponse.id)")
        }
        
        for modifiedTrackResponse in content.modified {
            let modifiedTrackId = modifiedTrackResponse.id
            GGSyncLog.debug("Modifying track with ID \(modifiedTrackId)")
            let oldTrack = TrackDao.findById(modifiedTrackId)!
            
            // Check if our caches are invalidated for this track
            var newSongCachedAt: Date? = nil
            if let songCachedAt = oldTrack.songCachedAt, let songUpdatedAt = modifiedTrackResponse.songUpdatedAt {
                if songCachedAt > songUpdatedAt {
                    newSongCachedAt = oldTrack.songCachedAt
                } else {
                    CacheService.deleteCachedData(trackId: oldTrack.id, cacheType: .song, ignoreDataUsageRecalculation: true)
                }
            }
            
            var newArtCachedAt: Date? = nil
            if let artCachedAt = oldTrack.artCachedAt, let artUpdatedAt = modifiedTrackResponse.artUpdatedAt {
                if artCachedAt > artUpdatedAt {
                    newArtCachedAt = oldTrack.artCachedAt
                } else {
                    CacheService.deleteCachedData(trackId: oldTrack.id, cacheType: .art, ignoreDataUsageRecalculation: true)
                }
            }
            
            var newThumbnailCachedAt: Date? = nil
            if let thumbnailCachedAt = oldTrack.thumbnailCachedAt, let thumbnailUpdatedAt = modifiedTrackResponse.artUpdatedAt {
                if thumbnailCachedAt > thumbnailUpdatedAt {
                    newThumbnailCachedAt = oldTrack.thumbnailCachedAt
                } else {
                    CacheService.deleteCachedData(trackId: oldTrack.id, cacheType: .thumbnail, ignoreDataUsageRecalculation: true)
                }
            }
            
            let updatedTrack = modifiedTrackResponse.asTrack(
                songCachedAt: newSongCachedAt,
                artCachedAt: newArtCachedAt,
                thumbnailCachedAt: newThumbnailCachedAt
            )
            
            TrackDao.save(updatedTrack)
            TrackService.broadcastTrackChange(updatedTrack)

            // A track SHOULD never go from not being in review, to being in review. But w/e. Sometimes I do weird stuff with the DB manually
            if oldTrack.inReview || updatedTrack.inReview {
                modifiedInReviewTracks.append(updatedTrack)
            }
        }

        for deletedId in content.removed {
            guard let deletedTrack = TrackDao.findById(deletedId) else {
                GGSyncLog.info("Deleted track with ID: \(deletedId) but it was already deleted. Making sure data is deleted from disk")
                CacheService.deleteAllData(trackId: deletedId)
                continue
            }
            if deletedTrack.inReview {
                deletedInReviewTracks.append(deletedTrack)
            }
            
            TrackDao.delete(deletedId)
            CacheService.deleteAllData(trackId: deletedId)
            
            GGSyncLog.debug("Deleted track with ID: \(deletedId)")
        }
        
        pagesToFetch = entityResponse!.pageable.totalPages
        
        // These things technically shouldn't have to run if there were no changes, but it is handy to have a place
        // that continually makes sure these things are correct, as something unexpected could have modified them.
        OfflineStorageService.recalculateUsedOfflineStorage()
        
        // The sync could have happened as a result of launching the app. Give the user some time to fiddle with
        // download settings, in case they want to stop the downloads from happening before we begin. Don't want
        // the delay to be too long though as we can't download in the background indefinitely
        DispatchQueue.global().asyncAfter(deadline: .now() + 8.0) {
            OfflineStorageService.downloadAlwaysOfflineMusic()
        }
        
        if !newInReviewTracks.isEmpty || !deletedInReviewTracks.isEmpty || !modifiedInReviewTracks.isEmpty {
            if let vc = reviewQueueController {
                vc.handleTrackChanges(
                    new: newInReviewTracks,
                    updated: modifiedInReviewTracks,
                    deleted: deletedInReviewTracks
                )
            } else {
                GGLog.debug("Review queue tracks were synced but the review queue controller was not registered")
            }
        }
        
        return (pagesToFetch, true)
    }
    
    static func registerReviewQueueController(_ vc: ReviewQueueController) {
        reviewQueueController = vc
    }
}

struct TrackResponse: Codable {
    let id: Int
    let name: String
    let artist: String
    let featuring: String
    let album: String
    let trackNumber: Int?
    let length: Int
    let releaseYear: Int?
    let genre: String?
    let playCount: Int
    let `private`: Bool
    let inReview: Bool
    let hidden: Bool
    let lastPlayed: Date?
    let addedToLibrary: Date?
    let note: String?
    let songUpdatedAt: Date?
    let artUpdatedAt: Date?
    let offlineAvailability: String
    let filesizeSongOgg: Int
    let filesizeSongMp3: Int
    let filesizeArtPng: Int
    let filesizeThumbnail64x64Png: Int
    let reviewSourceId: Int?
    let lastReviewed: Date?
    
    func asTrack(songCachedAt: Date? = nil, artCachedAt: Date? = nil, thumbnailCachedAt: Date? = nil) -> Track {
        return Track(
            id: id,
            album: album,
            artist: artist,
            addedToLibrary: addedToLibrary,
            featuring: featuring,
            genre: genre,
            isHidden: hidden,
            isPrivate: `private`,
            inReview: inReview,
            lastPlayed: lastPlayed,
            startedOnDevice: nil,
            lastReviewed: lastReviewed,
            length: length,
            name: name,
            note: note,
            playCount: playCount,
            releaseYear: releaseYear,
            trackNumber: trackNumber,
            songCachedAt: songCachedAt,
            artCachedAt: artCachedAt,
            thumbnailCachedAt: thumbnailCachedAt,
            offlineAvailability: OfflineAvailabilityType(rawValue: offlineAvailability) ?? .UNKNOWN,
            filesizeSongOgg: filesizeSongOgg,
            filesizeSongMp3: filesizeSongMp3,
            filesizeArtPng: filesizeArtPng,
            filesizeThumbnailPng: filesizeThumbnail64x64Png,
            reviewSourceId: reviewSourceId
        )
    }
}
