import Foundation
import UIKit

class LogViewController : UIViewController {
    
    private let logContent = UITextView()
    private let activitySpinner = UIActivityIndicatorView()
    
    private let logDisplaySize = 40_000 // Number of characters

    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Log Viewer"

        self.view.backgroundColor = Colors.background

        logContent.isEditable = false
        logContent.isScrollEnabled = true
        logContent.textColor = Colors.foreground
        logContent.backgroundColor = Colors.background
        logContent.font = logContent.font?.withSize(11)
        logContent.isHidden = true
        logContent.sizeToFit()
        
        activitySpinner.hidesWhenStopped = true
        activitySpinner.center = view.center
        activitySpinner.color = Colors.foreground
        
        self.view.addSubview(logContent)
        self.view.addSubview(activitySpinner)
        
        logContent.translatesAutoresizingMaskIntoConstraints = false
        logContent.heightAnchor.constraint(equalTo: view.heightAnchor).isActive = true
        logContent.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        GGNavLog.info("Loaded log view controller")
        activitySpinner.startAnimating()
        
        DispatchQueue.global().async {
            let logFile = GGLogger.createMergedLogFile()
            let logText = (try? String(contentsOf: logFile, encoding: .utf8)) ?? "No logs found. This is an error"
            
            let truncatedLogText = logText.substring(from: max(logText.count - self.logDisplaySize, 0))
            
            let simplifiedLogText = NSMutableAttributedString(string: "")
            
            let newLogRegex = try! NSRegularExpression(pattern: #"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}:\d{3} \[.+\] \[.+\] \[.+\]:"#)
            
            var color = Colors.foreground
            truncatedLogText
                .split(separator: "\n")
                .suffix(250) // Keep last 250 lines
                .forEach { line in
                    // If this is not a new log line, but a continuation of a previous log line that contained \n characters
                    // (such as formatted JSON output or whatever), then use the same color as the last log and call it a day
                    if !newLogRegex.matches(String(line)) {
                        let attributes: [NSAttributedString.Key: Any] = [.foregroundColor: color]
                        let attrString = NSAttributedString(string: "\n\(line)", attributes: attributes)
                        
                        simplifiedLogText.append(attrString)
                        return
                    }
                    
                    // This is a new log line. So trim the fat out keeping only the new timestamp, and color the content based off the log level
                    let logParts = line.split(separator: " ")
                    let time = logParts[safe: 1] ?? "TIME ERROR"
                    
                    if time == "TIME ERROR" {
                        GGLog.info(String(line))
                    }
                    // Default to crit because if we don't have the log parts, it probably means a log message got added
                    // that came outside the normal logger (like an index out of bounds error)
                    let logLevel = (logParts[safe: 4] ?? "[crit]:").dropLast(1)
                    
                    // dropFirst is safe to use if there are too many elements.
                    // This drops the date, time, line number, log tag, and debug level, leaving us with only the log itself remaining
                    let content = logParts.dropFirst(5).joined(separator: " ")
                    
                    if logLevel == "[warn]" {
                        color = Colors.warningYellow
                    } else if logLevel == "[error]" || logLevel == "[crit]" {
                        color = Colors.dangerRed
                    } else if logLevel == "[debug]" {
                        color = Colors.debugGrey
                    } else {
                        color = Colors.foreground
                    }
                    
                    let attributes: [NSAttributedString.Key: Any] = [.foregroundColor: color]

                    // Recombine the log message with the time, since that is actually useful information to have around
                    let attrString = NSAttributedString(string: "\n\(time): \(content)", attributes: attributes)
                    
                    simplifiedLogText.append(attrString)
                }
                        
            if FileManager.exists(logFile) {
                try! FileManager.default.removeItem(at: logFile)
            } else {
                GGLog.warning("Attempted to clean up log file but it was not found!")
            }
            
            DispatchQueue.main.async {
                self.logContent.attributedText = simplifiedLogText
                self.logContent.scrollToBottom()
                
                self.activitySpinner.stopAnimating()
                self.logContent.isHidden = false
            }
        }
    }
}
