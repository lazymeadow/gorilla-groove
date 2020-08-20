import Foundation
import CoreData

class TrackService {
    
    static func getTracks(
        album: String? = nil,
        artist: String? = nil,
        sortOverrideKey: String? = nil,
        sortAscending: Bool = true
    ) -> Array<Track> {
        let ownId = FileState.read(LoginState.self)!.id
        
        var sorts = [(String, Bool, Bool)]()
        
        // Sort differently depending on how we are trying to load things
        if (sortOverrideKey != nil) {
            sorts.append((sortOverrideKey!, sortAscending, true))
        } else if (artist != nil && album == nil) {
            sorts.append(("album", sortAscending, true))
            sorts.append(("track_number", true, false))
        } else if (album != nil) {
            sorts.append(("track_number", sortAscending, false))
        } else {
            sorts.append(("name", sortAscending, true))
        }
        
        return TrackDao.getTracks(userId: ownId, album: album, artist: artist, sorts: sorts)
    }
    
    // This doesn't update the play count. Waits for the server to sync it back down later. Possibly a mistake
    static func markTrackListenedTo(_ track: Track, _ retry: Int = 0) {
        if (retry > 3) {
            print("Failed to update track too many times. Giving up")
            return
        }
        
        // Update the server
        let postBody = MarkListenedRequest(
            trackId: track.id,
            deviceId: FileState.read(DeviceState.self)!.deviceId
        )
        HttpRequester.post("track/mark-listened", EmptyResponse.self, postBody) { _, statusCode ,_ in
            if (statusCode < 200 || statusCode >= 300) {
                print("Failed to mark track as listened to! For track with ID: \(track.id). Retrying...")
                self.markTrackListenedTo(track, retry + 1)
                return
            }
            
            print("Track \(track.id) marked listened to")
        }
    }
    
    struct MarkListenedRequest: Codable {
        let trackId: Int
        let deviceId: String
    }
}
