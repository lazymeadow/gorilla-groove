import Foundation

class ReviewSourceSynchronizer : StandardSynchronizer<ReviewSource, ReviewSourceResponse, ReviewSourceDao> {
   
}

struct ReviewSourceResponse: SyncResponseData {
    let id: Int
    let sourceType: SourceType
    let displayName: String
    
    func asEntity() -> Any {
        return ReviewSource(
            id: id,
            sourceType: sourceType,
            displayName: displayName
        )
    }
}
