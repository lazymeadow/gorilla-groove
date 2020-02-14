import Foundation
import CoreData

class UserState {
    private let coreDataManager: CoreDataManager
    private let context: NSManagedObjectContext
    
    func getOtherUsers() -> Array<User> {
        let ownId = FileState.read(LoginState.self)!.id
        
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "User")
        fetchRequest.predicate = NSPredicate(format: "id != \(ownId)")
        fetchRequest.sortDescriptors = [
            NSSortDescriptor(
                key: "name",
                ascending: true,
                selector: #selector(NSString.caseInsensitiveCompare)
            )
        ]
        let result = try! context.fetch(fetchRequest)
        
        return result as! Array<User>
    }
    
    func getLastSentUserSync() -> User {
        let ownId = FileState.read(LoginState.self)!.id
        
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "User")
        fetchRequest.predicate = NSPredicate(format: "id == \(ownId)")
        
        let result = try! context.fetch(fetchRequest)
        if (result.count > 0) {
            return result[0] as! User
        }

        // Save a new one. This is our first log in
        let entity = NSEntityDescription.entity(forEntityName: "User", in: context)
        let newUser = NSManagedObject(entity: entity!, insertInto: context)
        newUser.setValue(ownId, forKey: "id")
        
        do {
            try context.save()
        } catch {
            print("Failed to save new user sync!")
            print(error)
        }
        
        return newUser as! User
    }
    
    func updateUserSync(_ newSyncDate: NSDate) {
        context.refreshAllObjects()
        
        let user = getLastSentUserSync()
        user.setValue(newSyncDate, forKey: "last_sync")
        
        try! context.save()
    }
    
    func postCurrentDevice() {
        let requestBody = PostSessionRequest(deviceId: FileState.read(DeviceState.self)!.deviceId)
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
