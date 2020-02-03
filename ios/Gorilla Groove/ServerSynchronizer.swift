import Foundation
import CoreData

class ServerSynchronizer {
    let coreDataManager: CoreDataManager
    let context: NSManagedObjectContext
    let userSyncManager = UserSyncManager()
    
    typealias PageCompleteCallback = (_ completedPage: Int, _ totalPages: Int, _ entityType: AnyClass) -> Void
    let baseUrl = "sync/entity-type/%@/minimum/%ld/maximum/%ld?size=100&page="
    
    func syncWithServer(pageCompleteCallback: PageCompleteCallback? = nil) {
        print("Initiating sync with server...")
        
        let lastSync = userSyncManager.getLastSentUserSync()
        
        // API uses millis. Multiply by 1000
        let minimum = Int(lastSync.last_sync?.timeIntervalSince1970 ?? 0) * 1000
        let maximum = Int(NSDate().timeIntervalSince1970) * 1000
        let ownId = LoginState.read()!.id
        
        // TODO can I have 1 semaphore start at -1 and just use it for both?
        let semaphores = [DispatchSemaphore(value: 0), DispatchSemaphore(value: 0)]
        DispatchQueue.global().async {
            self.saveTracks(minimum, maximum, ownId, pageCompleteCallback, semaphores[0])
        }
        DispatchQueue.global().async {
            self.savePlaylists(minimum, maximum, ownId, pageCompleteCallback, semaphores[1])
        }

        semaphores.forEach { semaphore in
            semaphore.wait()
        }

        // Divide by 1000 to get back to seconds
        let newDate = NSDate(timeIntervalSince1970: Double(maximum) / 1000.0)
        
        // The user sync was pulled out using a different context, so save it using that context
        // TODO make a context singleton maybe to avoid this tomfoolery
        lastSync.setValue(newDate, forKey: "last_sync")
        userSyncManager.save()
        
        try! self.context.save()
    }
    
    // -- TRACKS
    
    private func saveTracks(
        _ minimum: Int,
        _ maximum: Int,
        _ ownId: Int,
        _ pageCompleteCallback: PageCompleteCallback?,
        _ allDoneSemaphore: DispatchSemaphore
    ) {
        let url = String(format: baseUrl, "TRACK", minimum, maximum)
        
        var currentPage = 0
        var pagesToGet = 0
        repeat {
            pagesToGet = savePageOfTrackChanges(url: url, page: currentPage, userId: ownId)
            pageCompleteCallback?(currentPage, pagesToGet, Track.self)
            currentPage += 1
        } while (currentPage < pagesToGet)
        
        print("Finished syncing tracks")
        allDoneSemaphore.signal()
    }
    
    private func savePageOfTrackChanges(url: String, page: Int, userId: Int) -> Int {
        let semaphore = DispatchSemaphore(value: 0)
        var pagesToFetch = -1
        HttpRequester.get(
            url + String(page),
            EntityChangeResponse<TrackResponse>.self
        ) { entityResponse, status, err in
            
            if (status < 200 || status >= 300 || entityResponse == nil) {
                print("Failed to sync new data!")
                semaphore.signal()
                
                return
            }
            
            for newTrackResponse in entityResponse!.content.new {
                let entity = NSEntityDescription.entity(forEntityName: "Track", in: self.context)
                let newTrack = NSManagedObject(entity: entity!, insertInto: self.context)
                newTrack.setValue(userId, forKey: "user_id")
                
                self.setTrackEntityPropertiesFromResponse(newTrack, newTrackResponse)
                
                print("Adding new track with ID: \((newTrack as! Track).id)")
            }
            
            for modifiedTrackResponse in entityResponse!.content.modified {
                let savedTrack = self.findEntityById(modifiedTrackResponse.id, Track.self)
                if (savedTrack == nil) {
                    print("Could not find Track to update with ID \(modifiedTrackResponse.id)!")
                    continue
                }
                
                self.setTrackEntityPropertiesFromResponse(savedTrack!, modifiedTrackResponse)
                
                print("Updating existing track with ID: \((savedTrack!).id)")
            }
            
            for deletedId in entityResponse!.content.removed {
                let savedTrack = self.findEntityById(deletedId, Track.self)
                if (savedTrack == nil) {
                    print("Could not find Track to delete with ID \(deletedId)!")
                    continue
                }
                
                self.context.delete(savedTrack!)
                
                print("Deleting track with ID: \(deletedId)")
            }
            
            pagesToFetch = entityResponse!.pageable.totalPages
            semaphore.signal()
        }
        
        semaphore.wait()
        
        return pagesToFetch
    }
    
    private func findEntityById<T>(_ id: Int64, _ type: T.Type) -> T? {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: String(describing: T.self))
        fetchRequest.predicate = NSPredicate(format: "id == \(id)")
        
        let result = try? self.context.fetch(fetchRequest)
        if (result == nil || result!.count == 0) {
            return nil
        }
        
        return result![0] as? T
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

    // -- PLAYLISTS
    
    private func savePlaylists(
        _ minimum: Int,
        _ maximum: Int,
        _ ownId: Int,
        _ pageCompleteCallback: PageCompleteCallback?,
        _ allDoneSemaphore: DispatchSemaphore
    ) {
        let url = String(format: baseUrl, "PLAYLIST", minimum, maximum)
        
        var currentPage = 0
        var pagesToGet = 0
        repeat {
            pagesToGet = savePageOfPlaylistChanges(url: url, page: currentPage, userId: ownId)
            pageCompleteCallback?(currentPage, pagesToGet, Playlist.self)
            currentPage += 1
        } while (currentPage < pagesToGet)
        
        print("Finished syncing playlists")
        allDoneSemaphore.signal()
    }
    
    private func savePageOfPlaylistChanges(url: String, page: Int, userId: Int) -> Int {
        let semaphore = DispatchSemaphore(value: 0)
        var pagesToFetch = -1
        HttpRequester.get(
            url + String(page),
            EntityChangeResponse<PlaylistResponse>.self
        ) { entityResponse, status, err in
            if (status < 200 || status >= 300 || entityResponse == nil) {
                print("Failed to sync new playlist data!")
                semaphore.signal()
                
                return
            }
            
            for newPlaylistResponse in entityResponse!.content.new {
                let entity = NSEntityDescription.entity(forEntityName: "Playlist", in: self.context)
                
                let newPlaylist = NSManagedObject(entity: entity!, insertInto: self.context)
                
                // TODO this WILL NOT WORK when shared playlists are more of a thing!
                newPlaylist.setValue(userId, forKey: "user_id")
                
                self.setPlaylistPropertiesFromResponse(newPlaylist, newPlaylistResponse)
                
                print("Adding new Playlist with ID: \((newPlaylist as! Playlist).id)")
            }
            
            for modifiedPlaylistResponse in entityResponse!.content.modified {
                let savedPlaylist = self.findEntityById(modifiedPlaylistResponse.id, Playlist.self)
                if (savedPlaylist == nil) {
                    print("Could not find Playlist to update with ID \(modifiedPlaylistResponse.id)!")
                    continue
                }
                
                self.setPlaylistPropertiesFromResponse(savedPlaylist!, modifiedPlaylistResponse)
                
                print("Updating existing track with ID: \((savedPlaylist!).id)")
            }
            
            for deletedId in entityResponse!.content.removed {
                let savedPlaylist = self.findEntityById(deletedId, Playlist.self)
                if (savedPlaylist == nil) {
                    print("Could not find Playlist to delete with ID \(deletedId)!")
                    continue
                }
                
                self.context.delete(savedPlaylist!)
                
                print("Deleting track with ID: \(deletedId)")
            }
            
            pagesToFetch = entityResponse!.pageable.totalPages
            semaphore.signal()
        }
        
        semaphore.wait()
        
        return pagesToFetch
    }
    
    private func setPlaylistPropertiesFromResponse(
        _ playlist: NSManagedObject,
        _ playlistResponse: PlaylistResponse
    ) {
        playlist.setValue(playlistResponse.id, forKey: "id")
        playlist.setValue(playlistResponse.name, forKey: "name")
        playlist.setValue(playlistResponse.createdAt, forKey: "created_at")
        playlist.setValue(playlistResponse.updatedAt, forKey: "updated_at")
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
    
    struct PlaylistResponse: Codable {
        let id: Int64
        let name: String
        let createdAt: Date
        let updatedAt: Date
    }
    
    struct PlaylistTrackResponse: Codable {
        let id: Int64
        let track: TrackResponse
        let playlistId: Int64
        let createdAt: Date
        let updatedAt: Date
    }
    
    struct UserResponse: Codable {
        let id: Int64
        let username: String
        let lastLogin: Date
        let createdAt: Date
        let updatedAt: Date
    }
    
    struct EntityChangeResponse<T: Codable>: Codable {
        let content: EntityChangeContent<T>
        let pageable: EntitySyncPagination
    }
    
    struct EntityChangeContent<T: Codable>: Codable {
        let new: Array<T>
        let modified: Array<T>
        let removed: Array<Int64>
    }
    
    struct EntitySyncPagination: Codable {
        let offset: Int
        let pageSize: Int
        let pageNumber: Int
        let totalPages: Int
        let totalElements: Int
    }
}
