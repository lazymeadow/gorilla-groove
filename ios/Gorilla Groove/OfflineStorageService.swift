import Foundation

class OfflineStorageService {
    
    @SettingsBundleStorage(key: "offline_storage_used")
    private static var offlineStorageUsed: String
    
    @SettingsBundleStorage(key: "offline_storage_fetch_mode")
    private static var offlineStorageFetchMode: OfflineStorageFetchModeType
    
    @SettingsBundleStorage(key: "max_offline_storage")
    private static var maxOfflineStorage: Int
    
    @SettingsBundleStorage(key: "offline_storage_enabled")
    private static var offlineStorageEnabled: Bool
    
    static func recalculateUsedOfflineStorage() {
        let priorDataStored = FileState.read(DataStored.self) ?? DataStored(stored: 0)
        
        let currentDataStored = TrackDao.getTotalBytesStored()
        let currentDataString = formatBytesString(currentDataStored)
        
        GGLog.info("Stored bytes went from \(formatBytesString(priorDataStored.stored)) to \(currentDataString)")
        
        FileState.save(DataStored(stored: currentDataStored))
        offlineStorageUsed = currentDataString
    }
    
    private static func formatBytesString(_ bytes: Int) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .decimal
        
        return formatter.string(fromByteCount: Int64(bytes))
    }
    
    static func canTrackBeCached(track: Track) -> Bool {
        return true
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
        OfflineStorageService.recalculateUsedOfflineStorage()
    }
    
    static func getCachedData(trackId: Int, cacheType: CacheType) -> Data? {
        let path = baseDir().appendingPathComponent(filenameFromCacheType(trackId, cacheType: cacheType))
        
        return try? Data(contentsOf: path)
    }
    
    static func deleteCachedData(trackId: Int, cacheType: CacheType, ignoreWarning: Bool = false) {
        let path = baseDir().appendingPathComponent(filenameFromCacheType(trackId, cacheType: cacheType))
        
        if FileManager.exists(path) {
            GGLog.info("Deleting cached \(cacheType) for track ID: \(trackId)")
            try! FileManager.default.removeItem(at: path)
        } else if !ignoreWarning {
            GGLog.warning("Attempted to deleting cached \(cacheType) for track ID: \(trackId) at path '\(path)' but it was not found")
        }
    }
    
    static func deleteAllData(trackId: Int) {
        CacheType.allCases.forEach { type in
            deleteCachedData(trackId: trackId, cacheType: type, ignoreWarning: true)
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
