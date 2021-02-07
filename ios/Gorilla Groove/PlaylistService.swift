import Foundation

class PlaylistService {
    static func removeTrack(_ playlistTrack: PlaylistTrack) {
        HttpRequester.delete("playlist/track?playlistTrackIds=\(playlistTrack.id)") { _, statusCode, _ in
            if statusCode.isSuccessful() {
                PlaylistTrackDao.delete(playlistTrack)
                broadcastPlaylistTrackChange(playlistTrack, type: .REMOVAL)
                Toast.show("Track removed")
            } else {
                Toast.show("Failed to remove the track!")
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
