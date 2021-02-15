import Foundation

class PlaylistService {
    static func removeTracks(_ tracks: [Track], playlist: Playlist) {
        let playlistTracks: [PlaylistTrack] = tracks.compactMap { track in
            let playlistTracksForTrack = PlaylistTrackDao.findByPlaylistAndTrack(playlistId: playlist.id, trackId: track.id)
            if playlistTracksForTrack.count > 1 {
                GGLog.warning("The user is deleting a playlist track that has duplicates. The one they are intending to delete may be different from the one that is deleted")
                return playlistTracksForTrack.first!
            } else if playlistTracksForTrack.isEmpty {
                GGLog.critical("No playlist tracks found when removing a track from a playlist! Track \(track.id). Playlist \(playlist.id)")
                return nil
            } else {
                return playlistTracksForTrack.first!
            }
        }
        
        PlaylistService.removeTracks(playlistTracks)
    }
    
    static func removeTracks(_ playlistTracks: [PlaylistTrack]) {
        let deleteStr = playlistTracks.map { $0.id.toString() }.joined(separator: ",")
        HttpRequester.delete("playlist/track?playlistTrackIds=\(deleteStr)") { _, statusCode, _ in
            if statusCode.isSuccessful() {
                playlistTracks.forEach { pt in
                    PlaylistTrackDao.delete(pt)
                    broadcastPlaylistTrackChange(pt, type: .REMOVAL)
                }
                
                let plurality = playlistTracks.count == 1 ? "Track" : "Tracks"
                Toast.show("\(plurality) removed")
            } else {
                let plurality = playlistTracks.count == 1 ? "track" : "tracks"
                Toast.show("Failed to remove the \(plurality)!")
            }
        }
    }
    
    private static var observers = [UUID : (PlaylistTrack, PlaylistTrackUpdateType) -> Void]()
    
    @discardableResult
    static func observePlaylistTrackChanges<T: AnyObject>(
        _ observer: T,
        closure: @escaping (T, PlaylistTrack, PlaylistTrackUpdateType) -> Void
    ) -> ObservationToken {
        let id = UUID()
        
        observers[id] = { [weak observer] track, changeType in
            guard let observer = observer else {
                observers.removeValue(forKey: id)
                return
            }

            closure(observer, track, changeType)
        }
        
        return ObservationToken {
            observers.removeValue(forKey: id)
        }
    }
    
    static func broadcastPlaylistTrackChange(_ track: PlaylistTrack, type: PlaylistTrackUpdateType) {
        observers.values.forEach { $0(track, type) }
    }
    
}

enum PlaylistTrackUpdateType {
    case REMOVAL
}
