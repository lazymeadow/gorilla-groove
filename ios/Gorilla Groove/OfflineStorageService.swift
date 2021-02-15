import Foundation
import Connectivity
import UIKit

fileprivate let logger = GGLogger(category: "storage")

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
    
    @SettingsBundleStorage(key: "offline_mode_enabled")
    static var offlineModeEnabled: Bool
    
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
        
        logger.debug("Enqueueing storage recalculation in \(delaySeconds) seconds")
        DispatchQueue.global().asyncAfter(deadline: .now() + delaySeconds) {
            if currentId == lastRecalculationId {
                OfflineStorageService.recalculateUsedOfflineStorage()
            } else {
                logger.debug("Not running delayed recalculation as another one has been enqueued")
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
            logger.debug("Stored bytes for cached songs went from \(currentBytesStored.toByteString()) to \(nextDataString)")
            currentBytesStored = nextBytesStored
        } else {
            logger.debug("Currently storing \(nextDataString) bytes of cached songs")
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
        
        if isDeviceStorageLow(additionalSpaceToUse: track.cacheSize) {
            return false
        }

        // There could potentially be logic in here to not do a cache if all data is already taken up with "always offline" songs, but
        // it seems like an unnecessary optimization. The track will just be immediately deleted right now after it is cached, which is
        // how it works anyway for a song you haven't played in a long time (or ever played) right now...
        return true
    }
    
    private static func isDeviceStorageLow(additionalSpaceToUse: Int = 0) -> Bool {
        // This is some paranoia, but I am adding in a buffer here (50 MB) to make this size check a bit more aggressive. I don't think there are
        // very many instances of people wanting to 100% max out the storage on their phone in order to store offline music....
        if additionalSpaceToUse + 50_000_000 > FileManager.systemFreeSizeBytes() {
            GGLog.warning("User is running out of storage space on their phone. \(FileManager.systemFreeSizeBytes()) bytes remaining")
            return true
        }
        
        return false
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
            logger.debug("Current stored bytes of \(currentBytesStored) are within the limit of \(maxOfflineStorageBytes). Not purging track cache")
            return
        }
        
        let bytesToPurge = currentBytesStored - maxOfflineStorageBytes
        
        logger.info("New cache limit of \(maxOfflineStorageBytes) is too small for current cache of \(currentBytesStored). Beginning cache purge of \(bytesToPurge) bytes")
        
        // Ordered by the last time they were played, with the first one being the track played the LEAST recently.
        let tracksToMaybePurge = TrackDao.getTracks(
            offlineAvailability: offlineAvailability,
            isCached: true,
            sorts: [("started_on_device", true, false)],
            // We recursively call this function until we've deleted enough.
            // Grab only 100 to keep memory usage down as this is basically a background task, and there's a good chance 100 is enough
            limit: 100
        )
        
        if tracksToMaybePurge.isEmpty {
            if offlineAvailability == .NORMAL {
                logger.info("Need to clear always offline song data in order to bring cache down!")
                return purgeExtraTrackDataIfNeeded(offlineAvailability: .AVAILABLE_OFFLINE)
            } else {
                logger.critical("Programmer error! Need to clear bytes, but no tracks are available to delete to clear up space?")
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
        
        logger.info("Purging \(tracksToPurge.count) tracks from cache")
        
        // There is a lot of potential here to bulk update these in the database. Lot of unnecessary calls here. But this is
        // currently always invoked in the background, and is not very time-sensitive or even likely to happen, as it requires the
        // user to reduce the size of storage allocated to GG which is quite rare. So I am being lazy and punting for now.
        tracksToPurge.forEach { track in
            CacheService.deleteCachedData(trackId: track.id, cacheType: .song, ignoreDataUsageRecalculation: true)
            CacheService.deleteCachedData(trackId: track.id, cacheType: .art, ignoreWarning: true, ignoreDataUsageRecalculation: true)
            
            // Need to broadcast the track change now that the cache has been purged so the views correctly remove them if offline mode is enabled.
            if let updatedTrack = TrackDao.findById(track.id) {
                TrackService.broadcastTrackChange(updatedTrack, type: .MODIFICATION)
            } else {
                GGLog.error("Could not find track after deleting its cache data!")
            }
        }
        
        // Invoke the recalculation just to make sure we've got the latest information about how much data we're using up now that
        // we have deleted stuff. Disable auto-purge since we're going to recursively call ourselves already
        OfflineStorageService.recalculateUsedOfflineStorage(purgeAfterRecalculation: false)
        
        if bytePurgeCount < bytesToPurge {
            logger.info("Did not clear enough bytes to push cache below the limit (cleared: \(bytePurgeCount) needed: \(bytesToPurge)). Doing another pass")
            purgeExtraTrackDataIfNeededInternal(offlineAvailability: offlineAvailability)
        }
    }

    private static var offlineMusicDownloadInProgress = false
    static var backgroundDownloadInterrupted: Bool = false
    
    static func downloadAlwaysOfflineMusic() {
        GGLog.debug("Running offline music download routine")
        if Thread.isMainThread {
            fatalError("Do not attempt to download music on the main thread!")
        }
        
        if offlineMusicDownloadInProgress {
            logger.debug("Always offline download process already in progress. Not starting another.")

            return
        }
        
        // This could take a long time. Don't let multiple threads do it. I put an additional boolean here to bail out
        // in case this takes a REALLY long time, so we don't have threads just pile up here.
        synchronized(self) {
            offlineMusicDownloadInProgress = true
            backgroundDownloadInterrupted = false

            let backgroundTask = UIApplication.shared.beginBackgroundTask {
                logger.info("Background download task was interrupted by the OS")
                // This lambda is the "interruption handler", invoked by the OS when it wants us to stop.
                // There are more fully fledged background tasks you can run separately that will notify your
                // application when they are done. But they are not well-suited for downloading lots of small files, one at a time
                backgroundDownloadInterrupted = true
                offlineMusicDownloadInProgress = false
            }
            downloadAlwaysOfflineMusicInternal()
            UIApplication.shared.endBackgroundTask(backgroundTask)

            offlineMusicDownloadInProgress = false
        }
    }
    
    private static func shouldDownloadMusic() -> Bool {
        if !offlineStorageEnabled {
            return false
        }
        
        if offlineModeEnabled {
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
            
            logger.debug("Checking WiFi connectivity...")

            let connectivity = Connectivity()
            connectivity.checkConnectivity { connectivity in
                wifiConnectivity = connectivity.status == .connectedViaWiFi
                connectivityCheckLock.signal()
            }
            
            connectivityCheckLock.wait()
            
            logger.debug("WiFi connectivity was: \(wifiConnectivity)")
            
            return wifiConnectivity
        }
    }
    
    private static func downloadAlwaysOfflineMusicInternal() {        
        logger.debug("Checking if always offline music should be downloaded...")
        if !shouldDownloadMusic() {
            return
        }
        
        let tracksNeedingCache = TrackDao.getTracks(
            offlineAvailability: .AVAILABLE_OFFLINE,
            isCached: false,
            sorts: [("filesize_song_mp3", true, false)]
        )
        
        if tracksNeedingCache.isEmpty {
            logger.debug("No always offline music to download")
            return
        }
        
        let maxStoragePerAvailability = TrackDao.getOfflineAvailabilityMaxStorage()
        let offlineMusicBytesNeeded = maxStoragePerAvailability[.AVAILABLE_OFFLINE]!
        
        // If this returns false, it means we needed to prompt the user for an action and should not proceed
        if !handleMaxStorageTooSmallAlert(maxStoragePerAvailability, offlineMusicBytesNeeded) {
            return
        }
        
        // We only care about the bytes that have been used for 'available offline' music, as any storage used up
        // for temporary cached songs does not take priority, and will be deleted to make room for these
        let offlineMusicBytesUsed = TrackDao.getOfflineAvailabilityCurrentStorage()[.AVAILABLE_OFFLINE]!
        
        // Now we know that we need to download some music. But we might not have enough space allocated to download it all.
        // We need to know how much space we have allocated for "always offline" music, and download untli we hit that limit
        var bytesAvailableToDownload = maxOfflineStorageBytes - offlineMusicBytesUsed
        
        var i = 0
        for track in tracksNeedingCache {
            if backgroundDownloadInterrupted {
                logger.info("Ending track downloads due to background task ending. Remaining time: \(UIApplication.shared.backgroundTimeRemaining)")
                break
            }
            
            if bytesAvailableToDownload < track.cacheSize {
                // Tracks are sorted in ascending size. So as soon as we encounter a track we can't download, we can't download any of them.
                logger.info("Not enough space remaining to download track \(track.id). Aborting further downloads")
                break
            }
            
            if isDeviceStorageLow(additionalSpaceToUse: track.cacheSize) {
                break
            }
            
            // Checking for WiFi connectivity is a bit of an expensive operation, so only check every 5 tracks.
            // It is fairly unlikely that the connectivity changed, so doing it every track is probably overkill anyway.
            i += 1
            if i % 5 == 0 {
                if !shouldDownloadMusic() {
                    logger.info("Music download conditions changed in the middle of downloading tracks. Aborting downloads")
                    break
                }
            }
            
            let bytesDownloaded = OfflineStorageService.downloadTrack(track)
            bytesAvailableToDownload -= bytesDownloaded

            // In downloading "always offline" music, we may now need to kick out temporary cached music if this put us over the limit.
            // It's a bit expensive to do, so only do it periodically, unless we are in danger of running out of storage on the phone.
            // If we are, then do it every time so we don't get screwed over. Recalculate the offline storage either way though. This
            // makes it so the user can watch the tracks get cached one at a time in the storage screen if they so choose to
            let recalculateStorage = i % 5 == 0 || FileManager.systemFreeSizeBytes() < 100_000_000
            recalculateUsedOfflineStorage(purgeAfterRecalculation: recalculateStorage)
            
            logger.info("Cache of 'always offline' track with ID \(track.id) is done")
        }
        
        if backgroundDownloadInterrupted && i == tracksNeedingCache.count {
            logger.info("Background download was interrupted, but all tracks were finished downloading. Marking download as not interrupted")
            backgroundDownloadInterrupted = false
        }
    }
    
    private static func downloadTrack(_ track: Track) -> Int {
        logger.info("Beginning cache of 'always offline' track with ID \(track.id)")

        let songDownloadSemaphore = DispatchSemaphore(value: 0)
        let artDownloadSemaphore = DispatchSemaphore(value: 0)

        var bytesDownloadedForTrack = 0
        
        TrackService.fetchLinksForTrack(
            track: track,
            fetchSong: track.songCachedAt == nil,
            fetchArt: track.artCachedAt == nil && track.filesizeArtPng > 0
        ) { trackLinks in
            guard let trackLinks = trackLinks else {
                // Error case. Not logging as adequate error logging happens upstream
                songDownloadSemaphore.signal()
                artDownloadSemaphore.signal()
                return
            }
            
            if let songLink = trackLinks.songLink {
                HttpRequester.download(songLink) { fileOnDiskUrl in
                    if let fileOnDiskUrl = fileOnDiskUrl {
                        CacheService.setCachedData(trackId: track.id, fileOnDisk: fileOnDiskUrl, cacheType: .song)
                    }
                    
                    bytesDownloadedForTrack += track.filesizeSongMp3
                    songDownloadSemaphore.signal()
                }
            } else {
                songDownloadSemaphore.signal()
            }
            
            if let artLink = trackLinks.albumArtLink {
                HttpRequester.download(artLink) { fileOnDiskUrl in
                    if let fileOnDiskUrl = fileOnDiskUrl {
                        CacheService.setCachedData(trackId: track.id, fileOnDisk: fileOnDiskUrl, cacheType: .art)
                    }
                    
                    bytesDownloadedForTrack += track.filesizeArtPng
                    artDownloadSemaphore.signal()
                }
            } else {
                artDownloadSemaphore.signal()
            }
        }
        
        // Use semaphores here to let the downloads happen in parallel. But wait for them before continuing to the next track download
        songDownloadSemaphore.wait()
        artDownloadSemaphore.wait()
        
        logger.debug("\(bytesDownloadedForTrack) bytes were downloaded")
        return bytesDownloadedForTrack
    }
    
    // Boolean of whether or not to proceed with downloads
    private static func handleMaxStorageTooSmallAlert(
        _ maxStoragePerAvailability: [OfflineAvailabilityType: Int],
        _ offlineMusicBytes: Int
    ) -> Bool {
        if offlineMusicBytes > maxOfflineStorageBytes {
            logger.warning("More songs are marked available offline than space has been allocated for!")
            
            let userRejectedStorageIncrease = FileState.read(SizeIncreasePrompt.self)?.userRejectedIncrease ?? false
            
            if userRejectedStorageIncrease {
                logger.warning("User previously rejected the storage size increase. Not showing prompt")
                return true
            }
            logger.info("Showing storage size increase prompt")
            
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
                    logger.info("User elected to increase storage space to \(suggestedStorageString)")
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
    
    
    private static let disableOfflineModeButton: UIBarButtonItem = {
        let config = UIImage.SymbolConfiguration(pointSize: UIFont.systemFontSize * 1.2, weight: .medium, scale: .large)
        let icon = UIImage(systemName: "wifi.slash", withConfiguration: config)!
        
        return UIBarButtonItem(
            image: icon,
            style: .plain,
            action: {
                GGNavLog.info("Showing user offline mode disable prompt")

                ViewUtil.showAlert(title: "Go back online?", yesText: "Yes", dismissText: "Stay Offline") {
                    OfflineStorageService.offlineModeEnabled = false
                }
            }
        )
    }()
    
    static func addOfflineModeToggleObserverToVc(_ vc: UIViewController) {
        DispatchQueue.main.async {
            if offlineModeEnabled {
                addOfflineDisableButtonIfNeeded(vc)
            }
            
            SettingsService.observeOfflineModeChanged(vc) { vc, offlineModeEnabled in
                DispatchQueue.main.async {
                    if offlineModeEnabled {
                        addOfflineDisableButtonIfNeeded(vc)
                    } else {
                        if vc.navigationItem.leftBarButtonItems?.contains(disableOfflineModeButton) == true {
                            vc.navigationItem.leftBarButtonItems = vc.navigationItem.leftBarButtonItems!.filter { $0 != disableOfflineModeButton }
                        }
                    }
                }
            }
        }
    }
    
    private static func addOfflineDisableButtonIfNeeded(_ vc: UIViewController) {
        if vc.navigationController?.navigationBar.backItem != nil {
            return
        }
        
        var existingIcons = vc.navigationItem.leftBarButtonItems ?? []
        // If there is more than one icon in the view, don't add this one (or if it already exists for some reason)
        if existingIcons.count > 1 || existingIcons.contains(disableOfflineModeButton) {
            return
        }
        
        existingIcons.append(disableOfflineModeButton)
        
        vc.navigationItem.leftBarButtonItems = existingIcons
    }
}


class CacheService {
    static func baseDir() -> URL {
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
            logger.warning("We were caching data, but the file already exists. Deleting file, but this is probably unexpected")
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
            logger.info("Deleting cached \(cacheType) for track ID: \(trackId)")
            try! FileManager.default.removeItem(at: path)
        } else if !ignoreWarning {
            logger.warning("Attempted to deleting cached \(cacheType) for track ID: \(trackId) at path '\(path)' but it was not found")
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


fileprivate extension FileManager {
    static func systemFreeSizeBytes() -> Int64 {
        if let freeBytes = try? FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory())[.systemFreeSize] as! Int64 {
            return freeBytes
        } else {
            GGLog.critical("Could not calculate free bytes on device!")
            return 0
        }
    }
    
    static func systemSizeBytes() -> Int64 {
        if let freeBytes = try? FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory())[.systemSize] as! Int64 {
            return freeBytes
        } else {
            GGLog.critical("Could not calculate device storage capacity!")
            return 0
        }
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
