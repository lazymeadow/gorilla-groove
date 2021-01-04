import Foundation

class StandardSynchronizer<T: Entity, E: SyncResponseData, dao: BaseDao<T>> {
    
    static func sync(
        _ syncStatus: SyncStatus,
        _ maximum: Int,
        _ pageCompleteCallback: PageCompleteCallback?
    ) -> Bool {
        let url = String(format: ServerSynchronizer.baseUrl, syncStatus.syncType.rawValue, syncStatus.lastSynced.toEpochTime(), maximum)
        
        var currentPage = 0
        var pagesToGet = 0
        var success = true
        repeat {
            (pagesToGet, success) = syncPage(url: url, page: currentPage)
            if !success {
                GGSyncLog.error("Could not sync \(T.self) changes!")
                return false
            }
            pageCompleteCallback?(currentPage, pagesToGet, syncStatus.syncType)
            currentPage += 1
        } while (currentPage < pagesToGet)
       
        GGSyncLog.info("Finished syncing \(T.self)")
        return true
    }
    
    private static func syncPage(url: String, page: Int) -> (Int, Bool) {
        var pagesToFetch = -1
        let (entityResponse, status, _) = HttpRequester.getSync(
            url + String(page),
            EntityChangeResponse<E>.self
        )
        if (status < 200 || status >= 300 || entityResponse == nil) {
            GGSyncLog.error("Failed to sync new \(T.self) data!")
            
            return (-1, false)
        }
        
        for new in entityResponse!.content.new {
            dao.save(new.asEntity() as! T)
            GGSyncLog.debug("Adding new \(T.self) with ID: \(new.id)")
        }
        
        for modified in entityResponse!.content.modified {
            dao.save(modified.asEntity() as! T)
            GGSyncLog.debug("Updating existing \(T.self) with ID: \(modified.id)")
        }
        
        for deletedId in entityResponse!.content.removed {
            dao.delete(deletedId)
            GGSyncLog.debug("Deleting \(T.self) with ID: \(deletedId)")
        }
        
        pagesToFetch = entityResponse!.pageable.totalPages
        
        return (pagesToFetch, true)
    }
}

protocol SyncResponseData: Codable {
    var id: Int { get }
    
    // Either I am not smart enough, or Swift is not powerful enough, to let me return a generic Entity type here
    func asEntity() -> Any
}
