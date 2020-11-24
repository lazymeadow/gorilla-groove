import Foundation
import ZIPFoundation

// There is potential here to try to detect app crashes and deal with them. But everything I tried had some sort of issue.
// Probably the least obtrusive thing is to keep state saved on whether or not our app considers itself "safe" to shutdown,
// which means our app is in the background and NOT playing music. If the app is playing music, or is in the foreground,
// then we are in a state where if the app is closed (without applicationWillTerminate closing it), it was probably bad.
// But this has high potential to cause noise. So I am going to try just doing manual reports for now and see where we end up.
class CrashReportService {
    
    static func initialize() {
        if !isDebuggerAttached() {
            redirectLogToDocuments()
        }
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
            GGLog.critical("Chaos. Mischief. Fatal Error.")
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
            GGLog.error("Could not create archive")
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
            GGLog.info("Adding entry to ZIP archive failed with error: \(error.localizedDescription)")
        }
        
        let archiveData = try! Data(contentsOf: archiveUrl)

        if FileManager.exists(archiveUrl) {
            GGLog.info("Cleaning up zip archive")
            try! FileManager.default.removeItem(at: archiveUrl)
        } else {
            GGLog.warning("Attempted to clean up zip archive but it was not found!")
        }
        
        HttpRequester.upload("crash-report", EmptyResponse.self, archiveData) { _, status, _ in
            if status >= 200 && status < 300 {
                GGLog.info("Crash report was uploaded")
            }
        }
    }
    
    static func redirectLogToDocuments() {
        let logPath = getLogPath().path
        
        freopen(logPath.cString(using: String.Encoding.ascii)!, "a+", stdout)
        freopen(logPath.cString(using: String.Encoding.ascii)!, "a+", stderr)
    }
    
    // https://christiantietze.de/posts/2019/07/swift-is-debugger-attached/
    private static func isDebuggerAttached() -> Bool {
        var debuggerIsAttached = false

        var name: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]
        var info: kinfo_proc = kinfo_proc()
        var info_size = MemoryLayout<kinfo_proc>.size

        let success = name.withUnsafeMutableBytes { (nameBytePtr: UnsafeMutableRawBufferPointer) -> Bool in
            guard let nameBytesBlindMemory = nameBytePtr.bindMemory(to: Int32.self).baseAddress else { return false }
            return -1 != sysctl(nameBytesBlindMemory, 4, &info, &info_size, nil, 0)
        }

        if !success {
            debuggerIsAttached = false
        }

        if !debuggerIsAttached && (info.kp_proc.p_flag & P_TRACED) != 0 {
            debuggerIsAttached = true
        }

        return debuggerIsAttached
    }
}

//func mySigAction(signal: Int32) {
//    print("Got a signal: \(signal)")
//    CrashReportService.writeCrashFile()
//}
