import Foundation
import ZIPFoundation

// There is potential here to try to detect app crashes and deal with them. But everything I tried had some sort of issue.
// Probably the least obtrusive thing is to keep state saved on whether or not our app considers itself "safe" to shutdown,
// which means our app is in the background and NOT playing music. If the app is playing music, or is in the foreground,
// then we are in a state where if the app is closed (without applicationWillTerminate closing it), it was probably bad.
// But this has high potential to cause noise. So I am going to try just doing manual reports for now and see where we end up.
class CrashReportService {
    
    private static var currentStdout = stdout
    private static var currentStderr = stderr
    
    @SettingsBundleStorage(key: "show_critical_error_notice")
    private static var showCriticalErrorNotice: Bool
    
    @SettingsBundleStorage(key: "automatic_error_reporting")
    private static var automaticErrorReporting: Bool
    
    static func initialize() {
        // CocoaLumberjack will output to the console (stdout) as well as output to a file.
        // This means that if we redirect stdout to the file, we're going to double up on our logging.
        // It seems like it could make sense to just not have Cocoalumberjack log to a file, and just
        // always log everything to stdout, but I want to utilize the log rotation functionality of
        // the library, and I don't know of another way to do so....
        if !isDebuggerRunning() {
            redirectStdLogsToGGLog()
        }
        setupErrorInterceptors()
    }
    
    static func redirectStdLogsToGGLog() {
        guard let logPath = GGLogger.getCurrentLogPath()?.path else { return }
        GGLog.info("Setting up redirects at path \(logPath)")
        
        // "stdout" and "stderr" are closed once they are freopened. We need to continually reassign the stream
        // to a new output file as the device logs are rotated. So keep a reference to the latest location of
        // the stream so we can continually point it at the new log path.
        // It seemed like dup2() could be used instead, but I had issues getting it to work.
        currentStdout = freopen(logPath.cString(using: String.Encoding.ascii)!, "a+", stdout)
        currentStderr = freopen(logPath.cString(using: String.Encoding.ascii)!, "a+", stderr)
        
        // Remove the buffer. This seems to improve the timing of the streams in relation to our own logs.
        // But it does seem like sometimes it's still off. Maybe can't be helped.
        setbuf(currentStdout, nil)
        setbuf(currentStderr, nil)
        
        // Because this stuff is really sketchballs and I might end up back here to try to debug / improve upon this in the future,
        // here is some stuff I was messing around with. I did have good results with "fflush" making stuff immediately appear.
        // But I still had issues getting ALL normal iOS output to appear without being hooked up to xcode. It seems like Apple's
        // stuff ceases to log to stdout when xcode is no longer involved in the process, so there is nothing to redirect at all.
//        fputs("hello from stdout\n", stdout)
//        fputs("hello from stderr\n", stderr)

//        fflush(stdout)
//        fflush(stderr)
    }
    
    static func isDebuggerRunning() -> Bool {
        var info = kinfo_proc()
        var mib : [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]
        var size = MemoryLayout<kinfo_proc>.stride
        let junk = sysctl(&mib, UInt32(mib.count), &info, &size, nil, 0)
        assert(junk == 0, "sysctl failed")
        return (info.kp_proc.p_flag & P_TRACED) != 0
    }
    
    // Lot of failed attempts I'm keeping in here. May revisit.
    private static func setupErrorInterceptors() {
        //        if let crashFile = CrashReportService.readCrashFile() {
        //            print("A crash file was found: \(crashFile)")
        //        }
                
        // This can catch some issues, but not a lot of the important ones.
        // Keeping a print statement in here just to see what kinds of errors actually trip it.
        NSSetUncaughtExceptionHandler() { exception in
            GGLog.critical(exception.description)
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
    
    static func promptProblemReport() {
        ViewUtil.showAlert(
            title: "Send problem report?",
            message: "This will send Gorilla Groove's app logs and database",
            yesText: "Send",
            dismissText: "No thanks"
        ) {
            CrashReportService.sendProblemReport()
        }
    }
    
    static func getProblemReportStatus() -> ProblemReporting {
        return FileState.read(ProblemReporting.self) ?? ProblemReporting()
    }
    
    static func sendAutomatedCrashReport(_ errorMessage: String) {
        // Not a lot of point in sending a crash report when the app is being directly monitored
        if isDebuggerRunning() {
            GGLog.info("Debugger is running. Not sending automated crash report. Crashing the app instead so you notice this!")
            fatalError(errorMessage)
        }
        
        var problemReportingStatus = getProblemReportStatus()
        let lastAutomatedReport = problemReportingStatus.lastAutomatedReport
        let now = Date()
        if lastAutomatedReport == nil || Calendar.current.dateComponents([.hour], from: lastAutomatedReport!, to: now).hour! > 6 {
            if !automaticErrorReporting {
                GGLog.info("Automatic error reporting is disabled. Not sending crash report")
                return
            }
            
            // Local variable cache it as this hits prefs and we will invoke it more than once
            let showNotice = showCriticalErrorNotice
            
            GGLog.info("About to send automated crash report. The user will\(showNotice ? " " : " not ")be notified of the error")
            sendProblemReport()
            
            problemReportingStatus.lastAutomatedReport = now
            FileState.save(problemReportingStatus)
            
            if showNotice {
                ViewUtil.showAlert(
                    title: "Critical Error",
                    message: "A critical error occurred. Gorilla Groove staff have been notified. Show error message?",
                    yesText: "Show"
                ) {
                    ViewUtil.showAlert(message: errorMessage, dismissText: "Oof")
                }
            }
        } else {
            GGLog.info("A critical error was logged, but the last automated report was too recent. Not sending crash report")
        }
    }
    
    static func sendManualProblemReport() -> Bool {
        let success = sendProblemReport()
        
        if success {
            var problemReportingStatus = getProblemReportStatus()
            problemReportingStatus.lastManualReport = Date()
            
            FileState.save(problemReportingStatus)
        }
        
        return success
    }
    
    @discardableResult
    private static func sendProblemReport() -> Bool {
        if Thread.isMainThread {
            fatalError("Do not send problem report on the main thread!")
        }
        
        let archiveUrl = FileManager.default
            .urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("crash-report.zip")
        
        guard let archive = Archive(url: archiveUrl, accessMode: .create) else {
            GGLog.error("Could not create archive")
            return false
        }
                
        let ownId = FileState.read(LoginState.self)!.id
        let logPath = GGLogger.getMergedLogFilePath()
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
        
        // There could be multiple logs. getMergedLogFilePath() merges them into a new file.
        // This should now be cleaned up as it is no longer useful.
        if FileManager.exists(logPath) {
            GGLog.info("Cleaning up merged log file")
            try! FileManager.default.removeItem(at: logPath)
        } else {
            GGLog.warning("Attempted to clean up merged log file but it was not found!")
        }
        
        let semaphore = DispatchSemaphore(value: 0)
        var success = false
        HttpRequester.upload("crash-report", EmptyResponse.self, archiveData) { _, status, _ in
            if status >= 200 && status < 300 {
                GGLog.info("Crash report was uploaded")
                success = true
            }
            semaphore.signal()
        }
        
        semaphore.wait()
        
        return success
    }
    
    struct ProblemReporting: Codable {
        var lastAutomatedReport: Date? = nil
        var lastManualReport: Date? = nil
    }
}

//func mySigAction(signal: Int32) {
//    print("Got a signal: \(signal)")
//    CrashReportService.writeCrashFile()
//}
