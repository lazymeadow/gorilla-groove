import Foundation

public class ReviewSource : Entity {
    
    public var id: Int
    public var sourceType: SourceType
    public var displayName: String
    public var offlineAvailability: OfflineAvailabilityType
    
    public init(
        id: Int,
        sourceType: SourceType,
        displayName: String,
        offlineAvailability: OfflineAvailabilityType
    ) {
        self.id = id
        self.sourceType = sourceType
        self.displayName = displayName
        self.offlineAvailability = offlineAvailability
    }
    
    public static func fromDict(_ dict: [String : Any?]) -> ReviewSource {
        return ReviewSource(
            id: dict["id"] as! Int,
            sourceType: SourceType(rawValue: dict["sourceType"] as! String) ?? .UNKNOWN,
            displayName: dict["displayName"] as! String,
            offlineAvailability: OfflineAvailabilityType(rawValue: dict["offlineAvailability"] as! String) ?? .UNKNOWN
        )
    }
}

public class ReviewSourceDao : BaseDao<ReviewSource> {
    static func getSources() -> Array<ReviewSource> {
        return queryEntities("SELECT * FROM review_source")
    }
}

public enum SourceType: String, Codable, DbEnum, CaseIterable {
    case USER_RECOMMEND
    case ARTIST
    case YOUTUBE_CHANNEL
    case UNKNOWN // Future API additions may not yet be mapped
    
    func getDbName() -> String {
        return rawValue
    }
}
