import Foundation
import CoreData

final class CoreDataManager {
    
    // This is really just for debugging, as it's nice to know where the sqlite file is located so we can
    // open it up in an external database viewer. But I don't want to log it out 10 times as that's annoying
    private static var hasLoggedDb = false
    
    private let modelName = "Groove"
    
    var restoredCleanly = true
    
    private(set) lazy var managedObjectContext: NSManagedObjectContext = {
        let managedObjectContext = NSManagedObjectContext(concurrencyType: .mainQueueConcurrencyType)
        
        managedObjectContext.persistentStoreCoordinator = self.persistentStoreCoordinator
        
        return managedObjectContext
    }()
    
    private lazy var managedObjectModel: NSManagedObjectModel = {
        guard let modelURL = Bundle.main.url(forResource: self.modelName, withExtension: "momd") else {
            fatalError("Unable to Find Data Model")
        }
        
        guard let managedObjectModel = NSManagedObjectModel(contentsOf: modelURL) else {
            fatalError("Unable to Load Data Model")
        }
        
        return managedObjectModel
    }()
    
    private lazy var persistentStoreCoordinator: NSPersistentStoreCoordinator = {
        let persistentStoreCoordinator = NSPersistentStoreCoordinator(managedObjectModel: self.managedObjectModel)
        
        let fileManager = FileManager.default
        let storeName = "\(self.modelName).sqlite"
        
        let documentsDirectoryURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        
        let persistentStoreURL = documentsDirectoryURL.appendingPathComponent(storeName)
        
        if (!CoreDataManager.hasLoggedDb) {
            print("Persistent store URL: " + persistentStoreURL.absoluteString)
            CoreDataManager.hasLoggedDb = true
        }
        
        if (!FileManager.default.fileExists(atPath: persistentStoreURL.path)) {
            print("Previous file does not exist. Will be creating a new Core Data stack")
            restoredCleanly = false
        }
        
        do {
            try persistentStoreCoordinator.addPersistentStore(
                ofType: NSSQLiteStoreType,
                configurationName: nil,
                at: persistentStoreURL,
                options: nil
            )
        } catch {
            print("Unable to Load Persistent Store. Dropping and trying to recreate")
            restoredCleanly = false
            try! FileManager.default.removeItem(at: persistentStoreURL)
            
            do {
                try persistentStoreCoordinator.addPersistentStore(
                    ofType: NSSQLiteStoreType,
                    configurationName: nil,
                    at: persistentStoreURL,
                    options: nil
                )
            } catch {
                fatalError("Failed to recreate Core Data")
            }
        }
        
        return persistentStoreCoordinator
    }()
}
