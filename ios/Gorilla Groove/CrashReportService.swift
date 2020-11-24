import Foundation
import ZIPFoundation

// There is potential here to try to detect app crashes and deal with them. But everything I tried had some sort of issue.
// Probably the least obtrusive thing is to keep state saved on whether or not our app considers itself "safe" to shutdown,
// which means our app is in the background and NOT playing music. If the app is playing music, or is in the foreground,
// then we are in a state where if the app is closed (without applicationWillTerminate closing it), it was probably bad.
// But this has high potential to cause noise. So I am going to try just doing manual reports for now and see where we end up.
class CrashReportService {
    
    static func initialize() {
        redirectLogToDocuments()
        setupErrorInterceptors()
    }
    
    // Lot of failed attempts I'm keeping in here. May revisit.
    private static func setupErrorInterceptors() {
        //        if let crashFile = CrashReportService.readCrashFile() {
        //            print("A crash file was found: \(crashFile)")
        //        }
                
        // This can catch some issues, but not a lot of the important ones.
        // Keeping a print statement in here just to see what kinds of errors actually trip it.
        NSSetUncaughtExceptionHandler() { exception in
            print("Chaos. Mischief. Fatal Error.")
        }
                
        //        let mySigAction: SigactionHandler = { type in
        //            print("Got a signal")
        //        }
                
        //        sigaction(SIGQUIT, mySigAction, NULL);
        //        signal(SIGTRAP, mySigAction)
        //        signal(SIGQUIT, mySigAction)
        //        signal(SIGTERM, mySigAction)
        //        signal(SIGKILL, mySigAction)
        //        signal(SIGSEGV, mySigAction)
        //        signal(SIGBUS, mySigAction)
        //        signal(SIGILL, mySigAction)
        //        signal(SIGINT, mySigAction)
        //        signal(SIGQUIT) { hey in
        //            print("Got a signal.")
        //        }
    }
    
    private static func getLogPath() -> URL {
        return FileManager.default
            .urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("app-log.txt")
    }
    
    static func sendProblemReport() {
        let archiveUrl = FileManager.default
            .urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("crash-report.zip")
        
        guard let archive = Archive(url: archiveUrl, accessMode: .create) else {
            print("Could not create archive")
            return
        }
        
        let ownId = FileState.read(LoginState.self)!.id
        let logPath = getLogPath()
        let dbPath = Database.getDbPath(ownId)
        
        do {
            // This thing has a not great API. Can't just pass in a complete Path. Have to take the complete path and tear
            // it into two pieces so the library can recombine it??
            try archive.addEntry(with: logPath.lastPathComponent, relativeTo: logPath.deletingLastPathComponent())
            try archive.addEntry(with: dbPath.lastPathComponent, relativeTo: dbPath.deletingLastPathComponent())
        } catch {
            print("Adding entry to ZIP archive failed with error: \(error)")
        }
        
        let archiveData = try! Data(contentsOf: archiveUrl)

        if FileManager.exists(archiveUrl) {
            print("Cleaning up zip archive")
            try! FileManager.default.removeItem(at: archiveUrl)
        } else {
            print("Attempted to clean up zip archive but it was not found!")
        }
        
        HttpRequester.upload("crash-report", EmptyResponse.self, archiveData) { _, status, _ in
            if status >= 200 && status < 300 {
                print("Crash report was uploaded")
            }
        }
    }
    
    static func redirectLogToDocuments() {
//        let allPaths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
//        let documentsDirectory = allPaths.first!
//        let logPath = "\(documentsDirectory)/app-log.txt"
        
//        print("Log path 1 \(logPath)")
//        print("Log path 2 \(getLogPath().path)")

        let logPath = getLogPath().path
        
//        freopen(logPath.cString(using: String.Encoding.ascii)!, "a+", stdout)
//        freopen(logPath.cString(using: String.Encoding.ascii)!, "a+", stderr)
    }
}

//func mySigAction(signal: Int32) {
//    print("Got a signal: \(signal)")
//    CrashReportService.writeCrashFile()
//}
