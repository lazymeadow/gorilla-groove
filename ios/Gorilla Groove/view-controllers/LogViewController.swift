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
        GGNavLog.warning("Test warning")
        GGNavLog.error("Test error\nThis error contains a new line")
        GGNavLog.info("Back to info log")
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
                .suffix(200) // Keep last 200 lines
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
                    let logLevel = (logParts[safe: 4] ?? "[debug]:").dropLast(1)
                    
                    // dropFirst is safe to use if there are too many elements.
                    // This drops the date, time, line number, log tag, and debug level, leaving us with only the log itself remaining
                    let content = logParts.dropFirst(5).joined(separator: " ")
                    
                    if logLevel == "[warn]" {
                        color = Colors.warningYellow
                    } else if logLevel == "[error]" || logLevel == "[crit]" {
                        color = Colors.dangerRed
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

extension UITextView {
    func scrollToBottom() {
        let textCount: Int = text.count
        guard textCount >= 1 else { return }
        scrollRangeToVisible(NSRange(location: textCount - 1, length: 1))
    }
}

extension String {
    func index(from: Int) -> Index {
        return self.index(startIndex, offsetBy: from)
    }

    func substring(from: Int) -> String {
        let fromIndex = index(from: from)
        return String(self[fromIndex...])
    }

    func substring(to: Int) -> String {
        let toIndex = index(from: to)
        return String(self[..<toIndex])
    }

    func substring(with r: Range<Int>) -> String {
        let startIndex = index(from: r.lowerBound)
        let endIndex = index(from: r.upperBound)
        return String(self[startIndex..<endIndex])
    }
}

extension NSRegularExpression {
    func matches(_ string: String) -> Bool {
        let range = NSRange(location: 0, length: string.utf16.count)
        return firstMatch(in: string, options: [], range: range) != nil
    }
}
