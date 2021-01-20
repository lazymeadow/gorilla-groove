import Foundation

class ReviewSourceSynchronizer : StandardSynchronizer<ReviewSource, ReviewSourceResponse, ReviewSourceDao> {
   
}

struct ReviewSourceResponse: SyncResponseData {
    let id: Int
    let sourceType: String
    let displayName: String
    let offlineAvailabilityType: String
    let active: Bool
    
    func asEntity() -> Any {
        return ReviewSource(
            id: id,
            sourceType: SourceType(rawValue: sourceType) ?? .UNKNOWN,
            displayName: displayName,
            offlineAvailability: OfflineAvailabilityType(rawValue: offlineAvailabilityType) ?? .UNKNOWN,
            active: active
        )
    }
}
