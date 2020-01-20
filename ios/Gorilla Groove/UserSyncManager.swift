import Foundation
import CoreData

class UserSyncManager {
    private let coreDataManager: CoreDataManager
    private let context: NSManagedObjectContext
    
    func getLastSentUserSync(_ ownId: Int) -> UserSync {
        print("Getting user sync for ID " + String(ownId))
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
        print("Setting new sync to be 0")
        newUserSync.setValue(NSDate(timeIntervalSince1970: 0), forKey: "last_sync")
        newUserSync.setValue(UUID(), forKey: "device_id")
        
        do {
            try context.save()
        } catch {
            print("Failed to save new user sync!")
            print(error)
        }
        
        return newUserSync as! UserSync
    }
    
    func save() {
        print("Saving UserSyncManager")
        try! context.save()
    }
    
    func postCurrentDevice() {
        let ownId = LoginState.read()!.id
        let userSync = getLastSentUserSync(ownId)
        
        // Don't have to actually lowercase this UUID for any real reason, but the other devices do it lower and I'm OCD
        let requestBody = PostSessionRequest(deviceId: userSync.device_id.uuidString.lowercased())
        HttpRequester.put("device", EmptyResponse.self, requestBody) { _, responseCode, _ in
            if (responseCode < 200 || responseCode >= 300) {
                print("Failed to inform the server of our current device!")
            } else {
                print("Posted current device to server")
            }
        }
    }
    
    init() {
        coreDataManager = CoreDataManager()
        context = coreDataManager.managedObjectContext
    }
    
    struct PostSessionRequest: Codable {
        let deviceId: String
        let deviceType: String = "IPHONE"
        let version: String = "0.1"
    }
}
