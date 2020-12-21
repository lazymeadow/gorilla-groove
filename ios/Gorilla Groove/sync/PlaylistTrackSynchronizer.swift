import Foundation

class PlaylistTrackSynchronizer : StandardSynchronizer<PlaylistTrack, PlaylistTrackResponse, PlaylistTrackDao> {

}

struct PlaylistTrackResponse: SyncResponseData {
    let id: Int
    let track: TrackResponse
    let playlistId: Int
    let createdAt: Date
    let updatedAt: Date
    
    func asEntity() -> Any {
        return PlaylistTrack(
            id: id,
            playlistId: playlistId,
            createdAt: createdAt,
            trackId: track.id
        )
    }
}
