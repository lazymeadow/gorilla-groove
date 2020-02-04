import Foundation
import CoreData

class TrackState {
    let coreDataManager: CoreDataManager
    let context: NSManagedObjectContext
    let userSyncManager = UserState()
    
    
    func getTracks() -> Array<Track> {
        let ownId = FileState.read(LoginState.self)!.id
        
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "Track")
        
        let idPredicate = NSPredicate(format: "user_id == \(ownId)")
        let hiddenPredicate = NSPredicate(format: "is_hidden == FALSE")

        let andPredicate = NSCompoundPredicate(type: .and, subpredicates: [idPredicate, hiddenPredicate])
        
        fetchRequest.predicate = andPredicate
        fetchRequest.sortDescriptors = [
            NSSortDescriptor(
                key: "name",
                ascending: true,
                selector: #selector(NSString.caseInsensitiveCompare)
            )
        ]
        let result = try! context.fetch(fetchRequest)
        
        return result as! Array<Track>
    }
    
    func getPlaylists() -> Array<Playlist> {
        let ownId = FileState.read(LoginState.self)!.id

        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "Playlist")
        fetchRequest.predicate = NSPredicate(format: "user_id == \(ownId)")
        fetchRequest.sortDescriptors = [
            NSSortDescriptor(
                key: "name",
                ascending: true,
                selector: #selector(NSString.caseInsensitiveCompare)
            )
        ]
        let result = try! context.fetch(fetchRequest)
        
        return result as! Array<Playlist>
    }
    
    func getTracksForPlaylist(_ playlistId: Int64) -> Array<Track> {
        let playlistTrackRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "PlaylistTrack")
        playlistTrackRequest.predicate = NSPredicate(format: "playlist_id == \(playlistId)")

        let playlistTracks = (try! context.fetch(playlistTrackRequest)) as! Array<PlaylistTrack>
        
        let trackRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "Track")
        let trackIds = playlistTracks.map { $0.track_id }
        trackRequest.predicate = NSPredicate(format: "id IN %@", trackIds)
        
        let tracks = (try! context.fetch(trackRequest)) as! Array<Track>
        var trackIdToTrack: [Int64: Track] = [:]
        tracks.forEach { track in trackIdToTrack[track.id] = track }
        
        // We can't return just the tracks. We need to map over the PlaylistTracks and swap them out.
        // Why? If we don't do this, we will stomp out duplicate tracks on the playlist, which could be intentional
        return playlistTracks.map { trackIdToTrack[$0.track_id]! }
    }
    
    func markTrackListenedTo(_ track: Track, _ retry: Int = 0) {
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
            
            track.play_count += 1 // Update the object reference

            let savedTrack = self.findTrackById(track.id)!
            savedTrack.setValue(savedTrack.play_count + 1, forKey: "play_count")
            
            try! self.context.save()
            print("Track \(track.id) marked listened to")
        }
    }
    
    private func findTrackById(_ id: Int64) -> Track? {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "Track")
        fetchRequest.predicate = NSPredicate(format: "id == \(id)")
        
        let result = try? self.context.fetch(fetchRequest)
        if (result == nil || result!.count == 0) {
            return nil
        }
        
        return result![0] as? Track
    }
    
    init() {
        coreDataManager = CoreDataManager()
        context = coreDataManager.managedObjectContext
    }
    
    struct MarkListenedRequest: Codable {
        let trackId: Int64
        let deviceId: String
    }
}

