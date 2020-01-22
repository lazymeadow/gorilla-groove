import Foundation
import CoreData

class TrackState {
    let coreDataManager: CoreDataManager
    let context: NSManagedObjectContext
    let userSyncManager = UserSyncManager()
    
    func syncWithServer() {
        print("Initiating sync with server...")
        
        let lastSync = userSyncManager.getLastSentUserSync()
        
        // API uses millis. Multiply by 1000
        let minimum = Int(lastSync.last_sync.timeIntervalSince1970) * 1000
        let maximum = Int(NSDate().timeIntervalSince1970) * 1000
        
        let url = "track/changes-between/minimum/\(minimum)/maximum/\(maximum)?size=10&page="
        
        let ownId = LoginState.read()!.id
        let pagesToGet = savePageOfChanges(url: url, page: 0, userId: ownId)
        
        var currentPage = 1
        while (currentPage < pagesToGet) {
            savePageOfChanges(url: url, page: currentPage, userId: ownId)
            currentPage += 1
        }

        // Divide by 1000 to get back to seconds
        let newDate = NSDate(timeIntervalSince1970: Double(maximum) / 1000.0)
        
        // The user sync was pulled out using a different context, so save it using that context
        // TODO make a context singleton maybe to avoid this tomfoolery
        lastSync.setValue(newDate, forKey: "last_sync")
        userSyncManager.save()
        
        try! self.context.save()
    }
    
    private func savePageOfChanges(url: String, page: Int, userId: Int) -> Int {
        let semaphore = DispatchSemaphore(value: 0)
        var pagesToFetch = -1
        HttpRequester.get(url + String(page), TrackChangeResponse.self) { trackResponse, status, err in
            if (status < 200 || status >= 300 || trackResponse == nil) {
                print("Failed to sync new data!")
                return
            }
            
            for newTrackResponse in trackResponse!.content.newTracks {
                let entity = NSEntityDescription.entity(forEntityName: "Track", in: self.context)
                let newTrack = NSManagedObject(entity: entity!, insertInto: self.context)
                newTrack.setValue(userId, forKey: "user_id")

                self.setTrackEntityPropertiesFromResponse(newTrack, newTrackResponse)
                
                print("Adding new track with ID: \((newTrack as! Track).id)")
            }
            
            for modifiedTrackResponse in trackResponse!.content.modifiedTracks {
                let savedTrack = self.findTrackById(modifiedTrackResponse.id)
                if (savedTrack == nil) {
                    print("Could not find Track to update with ID \(modifiedTrackResponse.id)!")
                    continue
                }
                
                self.setTrackEntityPropertiesFromResponse(savedTrack!, modifiedTrackResponse)
                
                print("Updating existing track with ID: \((savedTrack!).id)")
            }
            
            for deletedId in trackResponse!.content.removedTrackIds {
                let savedTrack = self.findTrackById(deletedId)
                if (savedTrack == nil) {
                    print("Could not find Track to delete with ID \(deletedId)!")
                    continue
                }
                
                self.context.delete(savedTrack!)
                
                print("Deleting track with ID: \(deletedId)")
            }

            pagesToFetch = trackResponse!.pageable.totalPages
            semaphore.signal()

        }
        
        semaphore.wait()
        
        return pagesToFetch
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
    
    private func setTrackEntityPropertiesFromResponse(_ track: NSManagedObject, _ trackResponse: TrackResponse) {
        track.setValue(trackResponse.id, forKey: "id")
        track.setValue(trackResponse.name, forKey: "name")
        track.setValue(trackResponse.album, forKey: "album")
        track.setValue(trackResponse.artist, forKey: "artist")
        track.setValue(trackResponse.featuring, forKey: "featuring")
        track.setValue(trackResponse.genre, forKey: "genre")
        track.setValue(trackResponse.note, forKey: "note")
        track.setValue(trackResponse.createdAt, forKey: "created_at")
        track.setValue(trackResponse.releaseYear, forKey: "release_year")
        track.setValue(trackResponse.trackNumber, forKey: "track_number")
        track.setValue(trackResponse.hidden, forKey: "is_hidden")
        track.setValue(trackResponse.private, forKey: "is_private")
        track.setValue(trackResponse.lastPlayed, forKey: "last_played")
        track.setValue(trackResponse.length, forKey: "length")
        track.setValue(trackResponse.playCount, forKey: "play_count")
    }
    
    func getTracks() -> Array<Track> {
        let ownId = LoginState.read()!.id
        
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "Track")
        fetchRequest.predicate = NSPredicate(format: "user_id == \(ownId)")
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "name", ascending: true)]
        let result = try! context.fetch(fetchRequest)
        
        return result as! Array<Track>
    }
    
    func markTrackListenedTo(_ track: Track) {
        track.play_count += 1 // Update the object reference
        // Now update the stored DB state
        
        // Update the server
        let userSync = userSyncManager.getLastSentUserSync()
        let postBody = MarkListenedRequest(trackId: track.id, deviceId: userSync.deviceIdAsString())
        HttpRequester.post("track/mark-listened", EmptyResponse.self, postBody) { _, statusCode ,_ in
            if (statusCode < 200 || statusCode >= 300) {
                print("Failed to mark track as listened to! For track with ID: " + String(track.id))
            }
            
            let savedTrack = self.findTrackById(track.id)!
            savedTrack.setValue(savedTrack.play_count + 1, forKey: "play_count")
            
            try! self.context.save()
        }
    }
    
    init() {
        coreDataManager = CoreDataManager()
        context = coreDataManager.managedObjectContext
    }
    

    struct TrackResponse: Codable {
        let id: Int64
        let name: String
        let artist: String
        let featuring: String
        let album: String
        let trackNumber: Int?
        let length: Int
        let releaseYear: Int?
        let genre: String?
        let playCount: Int
        let `private`: Bool
        let hidden: Bool
        let lastPlayed: Date?
        let createdAt: Date
        let note: String?
    }

    struct TrackChangeResponse: Codable {
        let content: TrackChangeContent
        let pageable: TrackChangePagination
    }

    struct TrackChangeContent: Codable {
        let newTracks: Array<TrackResponse>
        let modifiedTracks: Array<TrackResponse>
        let removedTrackIds: Array<Int64>
    }

    struct TrackChangePagination: Codable {
        let offset: Int
        let pageSize: Int
        let pageNumber: Int
        let totalPages: Int
        let totalElements: Int
    }

    struct MarkListenedRequest: Codable {
        let trackId: Int64
        let deviceId: String
    }
}

