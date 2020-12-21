import Foundation

public struct SyncStatus : Entity {
    public var id: Int
    public var syncType: SyncType
    public var lastSynced: Date
    public var lastSyncAttempted: Date
    
    public init(
        id: Int = 0,
        syncType: SyncType,
        lastSynced: Date = Date.minimum(),
        lastSyncAttempted: Date = Date.minimum()
    ) {
        self.id = id
        self.syncType = syncType
        self.lastSynced = lastSynced
        self.lastSyncAttempted = lastSyncAttempted
    }
    
    public static func fromDict(_ dict: [String : Any?]) -> SyncStatus {
        return SyncStatus(
            id: dict["id"] as! Int,
            syncType: SyncType(rawValue: dict["syncType"] as! String)!,
            lastSynced: (dict["lastSynced"] as! Int).toDate(),
            lastSyncAttempted: (dict["lastSyncAttempted"] as! Int).toDate()
        )
    }
}

public class SyncStatusDao : BaseDao<SyncStatus> {
    static func getStatusByTypes(_ types: Array<SyncType>) -> [SyncType: SyncStatus] {
        var statusByType: [SyncType: SyncStatus] = [:]

        let whereClause = types.map { "'\($0.getDbName())'" }.joined(separator: ",")
        queryEntities("SELECT * FROM sync_status WHERE sync_type IN (\(whereClause))").forEach { syncStatus in
            statusByType[syncStatus.syncType] = syncStatus
        }
        
        SyncType.allCases.forEach { type in
            if statusByType[type] == nil {
                statusByType[type] = SyncStatus(syncType: type)
            }
        }
        
        return statusByType
    }
}

public enum SyncType: String, Codable, DbEnum, CaseIterable {
    case track = "TRACK"
    case user = "USER"
    case playlist = "PLAYLIST"
    case playlistTrack = "PLAYLIST_TRACK"
    case reviewSource = "REVIEW_SOURCE"
    
    func getDbName() -> String {
        return rawValue
    }
}
