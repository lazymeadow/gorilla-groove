import Foundation
import CoreData

class UserSyncManager {
    private let coreDataManager: CoreDataManager
    private let context: NSManagedObjectContext
    
    func getLastSentUserSync() -> User {
        let ownId = LoginState.read()!.id
        
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "User")
        fetchRequest.predicate = NSPredicate(format: "user_id == \(ownId)")
        
        let result = try! context.fetch(fetchRequest)
        if (result.count > 0) {
            return result[0] as! User
        }

        // Save a new one. This is our first log in
        
        let entity = NSEntityDescription.entity(forEntityName: "User", in: context)
        let newUser = NSManagedObject(entity: entity!, insertInto: context)
        newUser.setValue(ownId, forKey: "user_id")
        
        // TOOD move device_id out of the user and onto the, you know, device
        newUser.setValue(UUID(), forKey: "device_id")
        
        do {
            try context.save()
        } catch {
            print("Failed to save new user sync!")
            print(error)
        }
        
        return newUser as! User
    }
    
    func save() {
        try! context.save()
    }
    
    func postCurrentDevice() {
        let user = getLastSentUserSync()
        
        let requestBody = PostSessionRequest(deviceId: user.deviceIdAsString())
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
