import Foundation
import CocoaLumberjackSwift

let GGLog = GGLogger(category: "standard")
let GGSyncLog = GGLogger(category: "sync")
let GGNavLog = GGLogger(category: "navigation")

class GGLogger {
    
    var category: String
    
    static let fileLogger = DDFileLogger(logFileManager: GGEventFileManager())
    
    init(category: String) {
        self.category = category
    }
    
    static func initialize() {
        let formatter = LogFormatter()
//        let consoleLogger = DDOSLogger.sharedInstance
        
        // If the debugger is running, emit to the console (stdout). We don't want to otherwise or else the file logger will
        // double up on logs as stdout is redirected to a file when the debugger isn't active.
        if CrashReportService.isDebuggerRunning() {
            let consoleLogger = DDTTYLogger.sharedInstance!
            consoleLogger.logFormatter = formatter
            DDLog.add(consoleLogger)
        }

                
        // The file is rolled when either of these is true. Either 1 week, or 10 MB exceeded.
        fileLogger.rollingFrequency = 60 * 60 * 168 // 1 week

        fileLogger.maximumFileSize = 10_737_418_240 // 10 MB
        fileLogger.logFileManager.maximumNumberOfLogFiles = 2
        fileLogger.logFormatter = LogFormatter()
        DDLog.add(fileLogger)
    }

    func trace(_ message: String, file: StaticString = #file, line: UInt = #line) {
        DDLogVerbose(message, file: file, line: line, tag: LogTag(category: category, logLevel: "trace"))
    }
    func debug(_ message: String, file: StaticString = #file, line: UInt = #line) {
        DDLogDebug(message, file: file, line: line, tag: LogTag(category: category, logLevel: "debug"))
    }
    func info(_ message: String, file: StaticString = #file, line: UInt = #line) {
        DDLogInfo(message, file: file, line: line, tag: LogTag(category: category, logLevel: "info"))
    }
    func warning(_ message: String, file: StaticString = #file, line: UInt = #line) {
        DDLogWarn(message, file: file, line: line, tag: LogTag(category: category, logLevel: "warn"))
    }
    func error(_ message: String, file: StaticString = #file, line: UInt = #line) {
        DDLogError(message, file: file, line: line, tag: LogTag(category: category, logLevel: "error"))
    }
    func critical(_ message: String, file: StaticString = #file, line: UInt = #line) {
        DDLogError(message, file: file, line: line, tag: LogTag(category: category, logLevel: "crit"))
        
        CrashReportService.sendAutomatedCrashReport(message)
    }
    
    struct LogTag {
        let category: String
        // You would think that calling DDLogVerbose would automatically set the level to .verbose, but it DOES NOT.
        // Have to explicitly set it or it isn't there to read in the log message formatter,
        // so may as well just make it a string and save ourselves some effort of converting log levels to strings later
        let logLevel: String
    }
    
    static func createMergedLogFile() -> URL {
        let logFilePaths = fileLogger.logFileManager.sortedLogFilePaths
        let urls = logFilePaths.map { stringPath in
            URL(string: stringPath.addingPercentEncoding(withAllowedCharacters: CharacterSet.urlQueryAllowed)!)!
        }
        let mergedLogPath = getMergedLogPath()
        
        try! FileManager.default.merge(files: urls, to: mergedLogPath)
        
        return mergedLogPath
    }
    
    private static func getMergedLogPath() -> URL {
        return FileManager.default
            .urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("merged-app-log.txt")
    }
    
    static func getCurrentLogPath() -> URL? {
        // This was NEVER EMPTY for me developing locally, even disconnected from the debugger / building on a 'Release' build
        // But as soon as I deployed to testflight, it was empty.
        // Same issue at this guy, that never got a resolution as of Dec 2020 https://github.com/CocoaLumberjack/CocoaLumberjack/issues/800
        guard let currentPathString = fileLogger.logFileManager.sortedLogFilePaths.first else {
            GGLog.error("Could not get current log path!")
            return nil
        }
        
        return URL(string: currentPathString.addingPercentEncoding(withAllowedCharacters: CharacterSet.urlQueryAllowed)!)!
    }
    
    // Because this library is not the greatest, we now have to customize it by using inheritance! Hooray!
    // Why have a format function or a didArchiveLogFile function that are standalone?? That would be TOO EASY.
    // Let's SUBCLASS STUFF INSTEAD. WOOHOO. At least this library was made 10 years ago so it has the correct mindset for the era...
    class LogFormatter: NSObject, DDLogFormatter {
        let dateFormatter: DateFormatter

        override init() {
            dateFormatter = DateFormatter()
            dateFormatter.formatterBehavior = .behavior10_4
            dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss:SSS"

            super.init()
        }
        
        func format(message logMessage: DDLogMessage) -> String? {
            let dateAndTime = dateFormatter.string(from: logMessage.timestamp)
            let tag = logMessage.tag as! LogTag
            
            return "\(dateAndTime) [\(logMessage.fileName):\(logMessage.line)] [\(tag.category)] [\(tag.logLevel)]: \(logMessage.message)"
        }
    }
    
    class GGEventFileManager: DDLogFileManagerDefault {
        override func didArchiveLogFile(atPath logFilePath: String, wasRolled: Bool) {
            if wasRolled {
                GGLog.info("Logs were rotated at path \(logFilePath)")
                
                // The file has been rolled, and we need to redirect stdout and stderr to the new log file.
                // If we don't, then all native iOS logging that we don't control will be written to the wrong log.
                CrashReportService.redirectStdLogsToGGLog()
            }
        }
    }
}

extension FileManager {
    func merge(files: [URL], to destination: URL, chunkSize: Int = 1000000) throws {
        FileManager.default.createFile(atPath: destination.path, contents: nil, attributes: nil)
        let writer = try FileHandle(forWritingTo: destination)
        try files.reversed().forEach({ partLocation in
            let reader = try FileHandle(forReadingFrom: partLocation)
            var data = reader.readData(ofLength: chunkSize)
            while data.count > 0 {
                writer.write(data)
                data = reader.readData(ofLength: chunkSize)
            }
            reader.closeFile()
        })
        writer.closeFile()
    }
}

