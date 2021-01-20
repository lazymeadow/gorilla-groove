import Foundation

class BackgroundTaskService {
    private static var idToTask: [Int: BackgroundTaskItem] = [:]
    
    private static var polling = false
    
    private static var lock = NSObject()
    
    static var tasks: [BackgroundTaskItem] {
        get {
            return Array(idToTask.values)
        }
    }
    
    private static weak var addTrackListController: AddTrackListController?
    static func registerAddTrackController(_ vc: AddTrackListController) {
        self.addTrackListController = vc
    }
    
    static func addBackgroundTasks(_ tasks: [BackgroundTaskItem]) {
        GGLog.info("Adding new background tasks with IDs \(tasks.map { $0.id })")
        synchronized(lock) {
            tasks.forEach { task in
                idToTask[task.id] = task
            }
            
            addTrackListController?.recalculateProgressText()
            
            if polling {
                GGLog.debug("Already polling for tasks. Not starting a new poll")
                return
            } else {
                GGLog.debug("Not already polling for tasks. Starting a new poll")
                polling = true
                pollUntilAllProcessed()
            }
        }
    }
    
    static func pollUntilAllProcessed() {
        let ids = idToTask.values
            .filter { $0.status == .PENDING || $0.status == .RUNNING }
            .map { $0.id.toString() }
            .joined(separator: ",")
        
        GGLog.debug("Polling for tasks with IDs \(ids)")

        HttpRequester.get("background-task?ids=\(ids)", BackgroundTaskResponse.self) { response, status, _ in
            guard let items = response?.items, status.isSuccessful() else {
                GGLog.error("Could not find background task statuses")
                return
            }
            
            items.forEach { item in
                idToTask[item.id] = item

                if item.status == .FAILED {
                    DispatchQueue.main.async {
                        GGLog.error("Failed to download '\(item.description)'")
                        Toast.show("Failed to download '\(item.description)'")
                    }
                    idToTask[item.id] = nil
                }
            }
            
            synchronized(lock) {
                let allDone = idToTask.values.allSatisfy { $0.status == .COMPLETE }
                GGLog.debug("Polling is \(allDone ? "done" : "not done")")
                if allDone {
                    let completedTasks = idToTask.values
                    
                    ServerSynchronizer.syncWithServer(syncTypes: [.track, .reviewSource], abortIfRecentlySynced: false)
                    
                    DispatchQueue.main.async {
                        if completedTasks.count == 1 {
                            Toast.show("Finished downloading '\(completedTasks.first!.description)'")
                        } else if completedTasks.count > 1 {
                            Toast.show("Finished downloading \(completedTasks.count) items")
                        }
                    }
                    
                    idToTask = [:]
                    polling = false
                } else {
                    DispatchQueue.global().asyncAfter(deadline: .now() + 20.0) {
                        pollUntilAllProcessed()
                    }
                }
                
                addTrackListController?.recalculateProgressText()
            }
        }
    }
}

struct BackgroundTaskItem : Codable {
    let id: Int
    var status: BackgroundProcessStatus
    let type: BackgroundProcessType
    let description: String
}

enum BackgroundProcessStatus : String, Codable {
    case PENDING
    case RUNNING
    case COMPLETE
    case FAILED
}

enum BackgroundProcessType : String, Codable {
    case YT_DOWNLOAD
    case NAMED_IMPORT
}

struct BackgroundTaskResponse : Codable {
    let items: [BackgroundTaskItem]
}
