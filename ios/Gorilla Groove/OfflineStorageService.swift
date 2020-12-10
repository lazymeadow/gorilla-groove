import Foundation
import Connectivity

class OfflineStorageService {
    
    @SettingsBundleStorage(key: "offline_storage_used")
    private static var offlineStorageUsed: String
    
    @SettingsBundleStorage(key: "offline_storage_fetch_mode")
    private static var offlineStorageFetchModeInt: Int
    
    @SettingsBundleStorage(key: "max_offline_storage")
    private static var maxOfflineStorage: Int
    
    @SettingsBundleStorage(key: "offline_storage_enabled")
    private static var offlineStorageEnabled: Bool
    
    @SettingsBundleStorage(key: "always_offline_cached")
    private static var alwaysOfflineSongsCached: String
    
    @SettingsBundleStorage(key: "tracks_temporarily_cached")
    private static var tracksTemporarilyCached: String
    
    private static var currentBytesStored = 0
    private static var maxOfflineStorageBytes: Int {
        get {
            // maxOfflineStorage is stored in MB. Need to convert to bytes to do a comparison.
            maxOfflineStorage * 1_000_000
        }
    }
    
    // Surely it must be possible to read the possible values from a "Multi Value" settings bundle.
    // I failed to find out how, though. So I have duplicated them in code and hopefully they won't get out of sync....
    // These are represented in MB, like the settings bundle versions.
    // 0 Represents "no cap"
    private static let possibleStorageSizes = [
        100, 250, 500, 1000, 2500, 5000, 10_000, 25_000, 50_000, 100_000, 0
    ]
    
    private static var offlineStorageFetchMode: OfflineStorageFetchModeType {
        get {
            // Swift's runtime reflection / generic protocols are very lacking, and I am unable to automatically convert
            // to an enum inside of the @SettingsBundleStorage propertyWrapper. So here we are.
            OfflineStorageFetchModeType(rawValue: offlineStorageFetchModeInt)!
        }
    }
    
    static func initialize() {
        // We read this a lot, so just keep it in memory
        currentBytesStored = FileState.read(DataStored.self)?.stored ?? 0
    }
    
    
    private static var lastRecalculationId = UUID()
    
    // Recalculating is a bit expensive, and in some circumstances we might recalculate many times in a short period of time.
    // This will effectively "batch" recalculations so we only do them once. For example, a user might be skipping through
    // songs while on a good internet connection, resulting in a lot of things being cached in a short period of time. We don't
    // need to recalculate immediately after every song is cached. There's not really a benefit to the user.
    static func enqueueDelayedStorageRecalculation(delaySeconds: Double) {
        let currentId = UUID()
        lastRecalculationId = currentId
        
        GGLog.debug("Enqueueing storage recalculation in \(delaySeconds) seconds")
        DispatchQueue.global().asyncAfter(deadline: .now() + delaySeconds) {
            if currentId == lastRecalculationId {
                OfflineStorageService.recalculateUsedOfflineStorage()
            } else {
                GGLog.debug("Not running delayed recalculation as another one has been enqueued")
            }
        }
    }
    
    static func recalculateUsedOfflineStorage(purgeAfterRecalculation: Bool = true) {
        // This makes several DB calls, and several writes to user prefs. It should never be invoked on the main thread.
        if Thread.isMainThread {
            fatalError("Do not call recalculate offline storage on the main thread!")
        }
        
        let nextBytesStored = TrackDao.getTotalBytesStored()
        let nextDataString = nextBytesStored.toByteString()
        
        if currentBytesStored != nextBytesStored {
            GGLog.debug("Stored bytes for cached songs went from \(currentBytesStored.toByteString()) to \(nextDataString)")
            currentBytesStored = nextBytesStored
        } else {
            GGLog.debug("Currently storing \(nextDataString) bytes of cached songs")
        }
        
        FileState.save(DataStored(stored: nextBytesStored))
        offlineStorageUsed = nextDataString
        
        let totalAvailabilityCounts = TrackDao.getOfflineAvailabilityCounts(cachedOnly: false)
        let usedAvailabilityCounts = TrackDao.getOfflineAvailabilityCounts(cachedOnly: true)
        
        alwaysOfflineSongsCached = "\(usedAvailabilityCounts[OfflineAvailabilityType.AVAILABLE_OFFLINE]!) / \(totalAvailabilityCounts[OfflineAvailabilityType.AVAILABLE_OFFLINE]!)"
        tracksTemporarilyCached = "\(usedAvailabilityCounts[OfflineAvailabilityType.NORMAL]!)"
        
        if purgeAfterRecalculation {
            purgeExtraTrackDataIfNeeded()
        }
    }
    
    static func shouldTrackBeCached(track: Track) -> Bool {
        if track.offlineAvailability == .ONLINE_ONLY || !offlineStorageEnabled {
            return false
        }
        
        // There could potentially be logic in here to not do a cache if all data is already taken up with "always offline" songs, but
        // it seems like an unnecessary optimization. The track will just be immediately deleted right now after it is cached, which is
        // how it works anyway for a song you haven't played in a long time (or ever played) right now...
        return true
    }

    // It's possible, though not likely, that two of these could attempt to run at once. I had it happen with a debugger paused
    // and it resulted in a lot of attempts to delete stuff that already existed. Not the end of the world, but undesirale.
    static func purgeExtraTrackDataIfNeeded(offlineAvailability: OfflineAvailabilityType = .NORMAL) {
        synchronized(self) {
            purgeExtraTrackDataIfNeededInternal(offlineAvailability: offlineAvailability)
        }
    }
    
    static func purgeExtraTrackDataIfNeededInternal(offlineAvailability: OfflineAvailabilityType = .NORMAL) {
        if currentBytesStored < maxOfflineStorageBytes {
            GGLog.debug("Current stored bytes of \(currentBytesStored) are within the limit of \(maxOfflineStorageBytes). Not purging track cache")
            return
        }
        
        let bytesToPurge = currentBytesStored - maxOfflineStorageBytes
        
        GGLog.info("New cache limit of \(maxOfflineStorageBytes) is too small for current cache of \(currentBytesStored). Beginning cache purge of \(bytesToPurge) bytes")
        
        let ownId = FileState.read(LoginState.self)!.id

        // Ordered by the last time they were played, with the first one being the track played the LEAST recently.
        let tracksToMaybePurge = TrackDao.getTracks(
            userId: ownId,
            offlineAvailability: offlineAvailability,
            isCached: true,
            sorts: [("started_on_device", true, false)],
            // We recursively call this function until we've deleted enough.
            // Grab only 100 to keep memory usage down as this is basically a background task, and there's a good chance 100 is enough
            limit: 100
        )
        
        if tracksToMaybePurge.isEmpty {
            if offlineAvailability == .NORMAL {
                GGLog.info("Need to clear always offline song data in order to bring cache down!")
                return purgeExtraTrackDataIfNeeded(offlineAvailability: .AVAILABLE_OFFLINE)
            } else {
                GGLog.critical("Programmer error! Need to clear bytes, but no tracks are available to delete to clear up space?")
                return
            }
        }

        var bytePurgeCount = 0;
        var tracksToPurge: Array<Track> = [];
        
        for track in tracksToMaybePurge {
            if bytePurgeCount > bytesToPurge {
                break
            }
            
            bytePurgeCount += track.cacheSize
            tracksToPurge.append(track)
        }
        
        GGLog.info("Purging \(tracksToPurge.count) tracks from cache")
        
        // There is a lot of potential here to bulk update these in the database. Lot of unnecessary calls here. But this is
        // currently always invoked in the background, and is not very time-sensitive or even likely to happen, as it requires the
        // user to reduce the size of storage allocated to GG which is quite rare. So I am being lazy and punting for now.
        tracksToPurge.forEach { track in
            CacheService.deleteCachedData(trackId: track.id, cacheType: .song, ignoreDataUsageRecalculation: true)
            CacheService.deleteCachedData(trackId: track.id, cacheType: .art, ignoreWarning: true, ignoreDataUsageRecalculation: true)
        }
        
        // Invoke the recalculation just to make sure we've got the latest information about how much data we're using up now that
        // we have deleted stuff. Disable auto-purge since we're going to recursively call ourselves already
        OfflineStorageService.recalculateUsedOfflineStorage(purgeAfterRecalculation: false)
        
        if bytePurgeCount < bytesToPurge {
            GGLog.info("Did not clear enough bytes to push cache below the limit (cleared: \(bytePurgeCount) needed: \(bytesToPurge)). Doing another pass")
            purgeExtraTrackDataIfNeededInternal(offlineAvailability: offlineAvailability)
        }
    }

    private static var offlineMusicDownloadInProgress = false
    
    static func downloadAlwaysOfflineMusic() {
        if Thread.isMainThread {
            fatalError("Do not attempt to download music on the main thread!")
        }
        
        if offlineMusicDownloadInProgress {
            GGLog.debug("Always offline download process already in progress. Not starting another.")

            return
        }
        
        // This could take a long time. Don't let multiple threads do it. I put an additional boolean here to bail out
        // in case this takes a REALLY long time, so we don't have threads just pile up here.
        synchronized(self) {
            offlineMusicDownloadInProgress = true
            
            downloadAlwaysOfflineMusicInternal()
            
            offlineMusicDownloadInProgress = false
        }
    }
    
    private static func shouldDownloadMusic() -> Bool {
        if !offlineStorageEnabled {
            return false
        }
        
        switch (offlineStorageFetchMode) {
        case .always:
            return true
        case .never:
            return false
        case .wifi:
            var wifiConnectivity = false
            
            let connectivityCheckLock = DispatchSemaphore(value: 0)
            
            GGLog.debug("Checking WiFi connectivity...")

            let connectivity = Connectivity()
            connectivity.checkConnectivity { connectivity in
                wifiConnectivity = connectivity.status == .connectedViaWiFi
                connectivityCheckLock.signal()
            }
            
            connectivityCheckLock.wait()
            
            GGLog.debug("WiFi connectivity was: \(wifiConnectivity)")
            
            return wifiConnectivity
        }
    }
    
    private static func downloadAlwaysOfflineMusicInternal() {
        let ownId = FileState.read(LoginState.self)!.id
        
        GGLog.debug("Checking if always offline music should be downloaded...")
        if !shouldDownloadMusic() {
            return
        }
        
        let tracksNeedingCache = TrackDao.getTracks(
            userId: ownId,
            offlineAvailability: .AVAILABLE_OFFLINE,
            isCached: false,
            sorts: [("filesize_song_mp3", true, false)]
        )
        
        if tracksNeedingCache.isEmpty {
            GGLog.debug("No always offline music to download")

            return
        }
        
        // If this returns false, it means we needed to prompt the user for an action and should not proceed
        if !handleMaxStorageTooSmall() {
            return
        }

        var i = 0
        for track in tracksNeedingCache {
            // Checking for WiFi connectivity is a bit of an expensive operation, so only check every 5 tracks.
            // It is fairly unlikely that the connectivity changed, so doing it every track is probably overkill anyway.
            i += 1
            if i % 5 == 0 {
                if !shouldDownloadMusic() {
                    GGLog.info("Music download conditions changed in the middle of downloading tracks. Aborting downloads")
                    break
                }
            }

            GGLog.info("Beginning cache of 'always offline' track with ID \(track.id)")
            let trackDownloadSemaphore = DispatchSemaphore(value: 0)
            
            TrackService.fetchLinksForTrack(
                track: track,
                fetchSong: true,
                fetchArt: track.artCachedAt == nil && track.filesizeArtPng > 0
            ) { trackLinks in
                guard let trackLinks = trackLinks else {
                    trackDownloadSemaphore.signal() // Error case. Not logging as the error HTTP logging happens upstream
                    return
                }
                
                guard let songLink = trackLinks.songLink else {
                    // If there was no song data, then shit has somehow hit the fan. Don't proceed. Error logging happens upstream
                    trackDownloadSemaphore.signal()
                    return
                }
                
                let songDownloadSemaphore = DispatchSemaphore(value: 0)
                let artDownloadSemaphore = DispatchSemaphore(value: 0)

                HttpRequester.download(songLink) { fileOnDiskUrl in
                    if let fileOnDiskUrl = fileOnDiskUrl {
                        CacheService.setCachedData(trackId: track.id, fileOnDisk: fileOnDiskUrl, cacheType: .song)
                    }

                    songDownloadSemaphore.signal()
                }
                
                if let artLink = trackLinks.albumArtLink {
                    HttpRequester.download(artLink) { fileOnDiskUrl in
                        if let fileOnDiskUrl = fileOnDiskUrl {
                            CacheService.setCachedData(trackId: track.id, fileOnDisk: fileOnDiskUrl, cacheType: .art)
                        }

                        artDownloadSemaphore.signal()
                    }
                } else {
                    artDownloadSemaphore.signal()
                }
                
                // Use semaphores here to let the downloads happen in parallel. But wait for them before continuing to the next track download
                songDownloadSemaphore.wait()
                artDownloadSemaphore.wait()
                
                trackDownloadSemaphore.signal()
            }
            
            trackDownloadSemaphore.wait()
            GGLog.info("Cache of 'always offline' track with ID \(track.id) is done")
        }
        
        // In downloading "always offline" music, we may now need to kick out temporary cached music if this put us over the limit
        recalculateUsedOfflineStorage()
    }
    
    // Boolean of whether or not to proceed with downloads
    private static func handleMaxStorageTooSmall() -> Bool {
        let maxStoragePerAvailability = TrackDao.getOfflineAvailabilityMaxStorage()
        
        let offlineMusicBytes = maxStoragePerAvailability[OfflineAvailabilityType.AVAILABLE_OFFLINE]!
        if offlineMusicBytes > maxOfflineStorageBytes {
            GGLog.warning("More songs are marked available offline than space has been allocated for!")
            
            let userRejectedStorageIncrease = FileState.read(SizeIncreasePrompt.self)?.userRejectedIncrease ?? false
            
            if userRejectedStorageIncrease {
                GGLog.warning("User previously rejected the storage size increase. Not showing prompt")
                return true
            }
            GGLog.info("Showing storage size increase prompt")
            
            // These are ordered smallest to largest, so the first one we find that is larger than the storage we need is the minimum
            let minimumPossibleStorageSize = possibleStorageSizes.first { storageValue in
                return storageValue * 1_000_000 > offlineMusicBytes
            } ?? 0 // 0 represents no cap
            
            let suggestedStorageString = minimumPossibleStorageSize > 0 ? (minimumPossibleStorageSize * 1_000_000).toByteString() : "[No Limit]"
            
            // Preemptively set it to true so if they ignore it they don't see it again. Will set to false if they accept
            FileState.save(SizeIncreasePrompt(userRejectedIncrease: true))
            
            ViewUtil.showAlert(
                title: "Storage Space Too Small",
                message: "You have \(offlineMusicBytes.toByteString()) music marked 'Available Offline' but only have allocated \(maxOfflineStorageBytes.toByteString()) of storage. Would you like to increase it to \(suggestedStorageString)?",
                yesText: "Sure",
                dismissText: "No"
            ) {
                DispatchQueue.global().async {
                    GGLog.info("User elected to increase storage space to \(suggestedStorageString)")
                    FileState.save(SizeIncreasePrompt(userRejectedIncrease: false))
                    
                    maxOfflineStorage = minimumPossibleStorageSize
                    
                    // Storage is now increased, so run this again
                    downloadAlwaysOfflineMusic()
                }
            }
            return false
        }
        
        return true
    }

    struct DataStored: Codable {
        let stored: Int
    }
    
    struct SizeIncreasePrompt: Codable {
        let userRejectedIncrease: Bool
    }
}


class CacheService {
    private static func baseDir() -> URL {
        return FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    private static func filenameFromCacheType(_ trackId: Int, cacheType: CacheType) -> String {
        switch (cacheType) {
        case .art:
            return "\(trackId).png"
        case .song:
            return "\(trackId).mp3"
        case .thumbnail:
            return "\(trackId)-small.png"
        }
    }
    
    static func setCachedData(trackId: Int, data: Data, cacheType: CacheType) {
        let path = baseDir().appendingPathComponent(filenameFromCacheType(trackId, cacheType: cacheType))
        
        try! data.write(to: path)
        
        TrackDao.setCachedAt(trackId: trackId, cachedAt: Date(), cacheType: cacheType)
    }
    
    static func setCachedData(trackId: Int, fileOnDisk: URL, cacheType: CacheType) {
        let path = baseDir().appendingPathComponent(filenameFromCacheType(trackId, cacheType: cacheType))
        
        if FileManager.exists(path) {
            GGLog.warning("We were caching data, but the file already exists. Deleting file, but this is probably unexpected")
            try! FileManager.default.removeItem(at: path)
        }
        try! FileManager.default.moveItem(at: fileOnDisk, to: path)
        TrackDao.setCachedAt(trackId: trackId, cachedAt: Date(), cacheType: cacheType)
    }
    
    static func getCachedData(trackId: Int, cacheType: CacheType) -> Data? {
        let path = baseDir().appendingPathComponent(filenameFromCacheType(trackId, cacheType: cacheType))
        
        return try? Data(contentsOf: path)
    }
    
    static func deleteCachedData(trackId: Int, cacheType: CacheType, ignoreWarning: Bool = false, ignoreDataUsageRecalculation: Bool = false) {
        let path = baseDir().appendingPathComponent(filenameFromCacheType(trackId, cacheType: cacheType))
        TrackDao.setCachedAt(trackId: trackId, cachedAt: nil, cacheType: cacheType)

        if FileManager.exists(path) {
            GGLog.info("Deleting cached \(cacheType) for track ID: \(trackId)")
            try! FileManager.default.removeItem(at: path)
        } else if !ignoreWarning {
            GGLog.warning("Attempted to deleting cached \(cacheType) for track ID: \(trackId) at path '\(path)' but it was not found")
        }
        
        if !ignoreDataUsageRecalculation && (cacheType == .song || cacheType == .art) {
            // We deleted data, so there shouldn't be a need to purge any additional data. Save some cycles and skip the check
            OfflineStorageService.recalculateUsedOfflineStorage(purgeAfterRecalculation: false)
        }
    }
    
    static func deleteAllData(trackId: Int, ignoreDataUsageRecalculation: Bool = false) {
        CacheType.allCases.forEach { type in
            deleteCachedData(trackId: trackId, cacheType: type, ignoreWarning: true, ignoreDataUsageRecalculation: true)
        }
        
        if !ignoreDataUsageRecalculation {
            // We deleted data, so there shouldn't be a need to purge any additional data. Save some cycles and skip the check
            OfflineStorageService.recalculateUsedOfflineStorage(purgeAfterRecalculation: false)
        }
    }
}



fileprivate extension Int {
    func toByteString() -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .decimal
        
        return formatter.string(fromByteCount: Int64(self))
    }
}


extension FileManager {
    static func exists(_ path: URL) -> Bool {
        return FileManager.default.fileExists(atPath: path.path)
    }
    
    static func move(_ oldPath: URL, _ newPath: URL) {
        try! FileManager.default.moveItem(atPath: oldPath.path, toPath: newPath.path)
    }
}

enum OfflineStorageFetchModeType: Int {
    case always = 2
    case wifi = 1
    case never = 0
}

enum CacheType: CaseIterable {
    case song
    case art
    case thumbnail
}

// https://stackoverflow.com/a/61458763/13175115
@discardableResult
public func synchronized<T>(_ lock: AnyObject, closure:() -> T) -> T {
    objc_sync_enter(lock)
    defer { objc_sync_exit(lock) }

    return closure()
}
