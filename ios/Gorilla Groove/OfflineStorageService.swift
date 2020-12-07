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
    
    static func recalculateUsedOfflineStorage(purgeAfterRecalculation: Bool = true) {
        // This makes several DB calls, and several writes to user prefs. It should never be invoked on the main thread.
        if Thread.isMainThread {
            fatalError("Do not call recalculate offline storage on the main thread!")
        }
        
        let nextBytesStored = TrackDao.getTotalBytesStored()
        let nextDataString = formatBytesString(nextBytesStored)
        
        if currentBytesStored != nextBytesStored {
            GGLog.debug("Stored bytes for cached songs went from \(formatBytesString(currentBytesStored)) to \(nextDataString)")
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
    
    private static func formatBytesString(_ bytes: Int) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .decimal
        
        return formatter.string(fromByteCount: Int64(bytes))
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

    
    static func purgeExtraTrackDataIfNeeded(offlineAvailability: OfflineAvailabilityType = .NORMAL) {
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
            sorts: [("last_played", true, false)],
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
            purgeExtraTrackDataIfNeeded(offlineAvailability: offlineAvailability)
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
            
            GGLog.debug("Checking if always offline music should be downloaded...")
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

        if !shouldDownloadMusic() {
            return
        }
        
        let tracksNeedingCache = TrackDao.getTracks(
            userId: ownId,
            offlineAvailability: .AVAILABLE_OFFLINE,
            isCached: false
        )
        
        if tracksNeedingCache.isEmpty {
            GGLog.debug("No always offline music to download")

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

    struct DataStored: Codable {
        let stored: Int
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
