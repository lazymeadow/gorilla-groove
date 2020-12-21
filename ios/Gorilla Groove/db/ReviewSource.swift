import Foundation

public class ReviewSource : Entity {
    public var id: Int
    public var sourceType: SourceType
    public var displayName: String
    
    public init(
        id: Int,
        sourceType: SourceType,
        displayName: String
    ) {
        self.id = id
        self.sourceType = sourceType
        self.displayName = displayName
    }
    
    public static func fromDict(_ dict: [String : Any?]) -> ReviewSource {
        return ReviewSource(
            id: dict["id"] as! Int,
            sourceType: SourceType(rawValue: dict["sourceType"] as! String) ?? SourceType.UNKNOWN,
            displayName: dict["displayName"] as! String
        )
    }
}

public class ReviewSourceDao : BaseDao<ReviewSource> {
//    static func getPlaylists(userId: Int) -> Array<Playlist> {
//        return queryEntities("SELECT * FROM playlist WHERE user_id = \(userId) ORDER BY name COLLATE NOCASE ASC")
//    }
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
