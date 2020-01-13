//
//  TrackState.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/11/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import Foundation
import CoreData

class TrackState {
    let coreDataManager: CoreDataManager
    let context: NSManagedObjectContext
    
    func syncWithServer() {
        let ownId = LoginState.read()!.id
        print("Initiating sync with server...")
        let lastSync = getLastSentUserSync(ownId)
        
        // API uses millis. Multiply by 1000
        let minimum = Int(lastSync.last_sync.timeIntervalSince1970) * 1000
        let maximum = Int(NSDate().timeIntervalSince1970) * 1000
        
        let url = "track/changes-between/minimum/\(minimum)/maximum/\(maximum)?size=10&page="
        
        let pagesToGet = savePageOfChanges(url: url, page: 0)
        
        var currentPage = 1
        while (currentPage < pagesToGet) {
            savePageOfChanges(url: url, page: currentPage)
            currentPage += 1
        }

        // Divide by 1000 to get back to seconds
        let newDate = NSDate(timeIntervalSince1970: Double(maximum) / 1000.0)
        lastSync.setValue(newDate, forKey: "last_sync")
        
        try! self.context.save()
    }
    
    private func savePageOfChanges(url: String, page: Int) -> Int {
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
                
                print("Updating existing track with ID: \((savedTrack as! Track).id)")
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
    
    private func findTrackById(_ id: Int) -> Track? {
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
    
    private func getLastSentUserSync(_ ownId: Int) -> UserSync {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "UserSync")
        fetchRequest.predicate = NSPredicate(format: "user_id == \(ownId)")
        
        let result = try! context.fetch(fetchRequest)
        if (result.count > 0) {
            return result[0] as! UserSync
        }

        // Save a new one. This is our first log in
        
        let entity = NSEntityDescription.entity(forEntityName: "UserSync", in: context)
        let newUserSync = NSManagedObject(entity: entity!, insertInto: context)
        newUserSync.setValue(ownId, forKey: "user_id")
        newUserSync.setValue(NSDate(timeIntervalSince1970: 0), forKey: "last_sync")
        
        try! context.save()
        
        return newUserSync as! UserSync
    }
    
    init() {
        coreDataManager = CoreDataManager()
        context = coreDataManager.managedObjectContext
    }
}

struct TrackResponse: Codable {
    let id: Int
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
    let removedTrackIds: Array<Int>
}

struct TrackChangePagination: Codable {
    let offset: Int
    let pageSize: Int
    let pageNumber: Int
    let totalPages: Int
    let totalElements: Int
}
