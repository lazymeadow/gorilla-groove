import Foundation

class PlaylistSynchronizer : StandardSynchronizer<Playlist, PlaylistResponse, PlaylistDao> {

}

struct PlaylistResponse: SyncResponseData {
    let id: Int
    let name: String
    let createdAt: Date
    let updatedAt: Date
    
    func asEntity() -> Any {
        return Playlist(
            id: id,
            createdAt: createdAt,
            updatedAt: updatedAt,
            name: name
        )
    }
}
