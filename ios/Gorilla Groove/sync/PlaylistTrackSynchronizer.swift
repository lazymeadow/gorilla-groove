import Foundation

class PlaylistTrackSynchronizer : StandardSynchronizer<PlaylistTrack, PlaylistTrackResponse, PlaylistTrackDao> {

}

struct PlaylistTrackResponse: SyncResponseData {
    let id: Int
    let track: TrackIdResponse
    let playlistId: Int
    let sortOrder: Int
    let createdAt: Date
    let updatedAt: Date
    
    func asEntity() -> Any {
        return PlaylistTrack(
            id: id,
            playlistId: playlistId,
            sortOrder: sortOrder,
            createdAt: createdAt,
            trackId: track.id
        )
    }
}

struct TrackIdResponse: Codable {
    let id: Int
}
