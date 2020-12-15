import Foundation
import UIKit
import Toast

class ErrorReportController : UIViewController {
    
    private let lastReportLabel = UILabel()
    private let activitySpinner = UIActivityIndicatorView()
    private let viewLogButton = GGButton()
    private let sendButton = GGButton()

    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Error Reporting"
        
        // The background is already the "background" color, because it inherits. But without explicitly setting
        // the background, some of the "more" menu's table lines bleed over during the transition and it looks ugly
        self.view.backgroundColor = Colors.background
        
        let heading = UILabel()
        heading.text = "Send a problem report?"
        heading.textColor = Colors.foreground
        heading.font = heading.font.withSize(22)
        heading.sizeToFit()
        heading.textAlignment = .center
        
        let explanation = UILabel()
        explanation.text = "This will send Gorilla Groove's app logs and database off for troubleshooting."
        explanation.textColor = Colors.foreground
        explanation.font = heading.font.withSize(14)
        explanation.sizeToFit()
        explanation.numberOfLines = 0
        
        lastReportLabel.font = heading.font.withSize(14)
        lastReportLabel.textColor = Colors.foreground
        lastReportLabel.sizeToFit()
        lastReportLabel.numberOfLines = 0
        
        viewLogButton.setTitle("View Logs", for: .normal)
        viewLogButton.addTarget(self, action: #selector(logViewPressed(sender:)), for: .touchUpInside)
        viewLogButton.backgroundColor = Colors.navControls
        viewLogButton.tintColor = Colors.foreground
        viewLogButton.layer.cornerRadius = 5
        viewLogButton.sizeToFit()
        
        sendButton.setTitle("Send Report", for: .normal)
        sendButton.addTarget(self, action: #selector(sendReportPressed(sender:)), for: .touchUpInside)
        sendButton.backgroundColor = Colors.navControls
        sendButton.tintColor = Colors.foreground
        sendButton.adjustsImageWhenDisabled = true
        sendButton.layer.cornerRadius = 5
        sendButton.sizeToFit()
        
        self.view.addSubview(heading)
        self.view.addSubview(explanation)
        self.view.addSubview(lastReportLabel)
        self.view.addSubview(sendButton)
        self.view.addSubview(viewLogButton)
        self.view.addSubview(activitySpinner)
        
        heading.translatesAutoresizingMaskIntoConstraints = false
        heading.topAnchor.constraint(equalTo: view.topAnchor, constant: 20).isActive = true
        heading.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
        
        explanation.translatesAutoresizingMaskIntoConstraints = false
        explanation.topAnchor.constraint(equalTo: heading.bottomAnchor, constant: 20).isActive = true
        explanation.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 10).isActive = true
        explanation.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -10).isActive = true
        
        lastReportLabel.translatesAutoresizingMaskIntoConstraints = false
        lastReportLabel.topAnchor.constraint(equalTo: explanation.bottomAnchor, constant: 20).isActive = true
        lastReportLabel.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 10).isActive = true
        lastReportLabel.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -10).isActive = true
        
        viewLogButton.translatesAutoresizingMaskIntoConstraints = false
        viewLogButton.topAnchor.constraint(equalTo: lastReportLabel.bottomAnchor, constant: 30).isActive = true
        viewLogButton.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 20).isActive = true
        viewLogButton.rightAnchor.constraint(equalTo: sendButton.leftAnchor, constant: -20).isActive = true
        
        sendButton.translatesAutoresizingMaskIntoConstraints = false
        sendButton.topAnchor.constraint(equalTo: lastReportLabel.bottomAnchor, constant: 30).isActive = true
        sendButton.leftAnchor.constraint(equalTo: viewLogButton.rightAnchor, constant: 20).isActive = true
        sendButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -20).isActive = true
        sendButton.widthAnchor.constraint(equalTo: viewLogButton.widthAnchor).isActive = true
        
        activitySpinner.hidesWhenStopped = true
        activitySpinner.center = view.center
        activitySpinner.color = Colors.foreground
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        GGNavLog.info("Loaded error report controller")
                
        lastReportLabel.text = getLastReportedErrorText()
        lastReportLabel.sizeToFit()
        
        sendButton.isDisabled = false
        viewLogButton.isDisabled = false
    }
    
    private func getLastReportedErrorText() -> String {
        let reportStatus = CrashReportService.getProblemReportStatus()
        
        if let manualReport = reportStatus.lastManualReport, let automatedReport = reportStatus.lastAutomatedReport {
            return "Your last manual problem report was sent \(manualReport.toTimeAgoString()). The last automated problem report was sent \(automatedReport.toTimeAgoString())"
        } else if let manualReport = reportStatus.lastManualReport {
            return "You previously sent in a manual problem report \(manualReport.toTimeAgoString())"
        } else if let automatedReport = reportStatus.lastAutomatedReport {
            return "An automated report was last sent \(automatedReport.toTimeAgoString())"
        } else {
            return "You have not previously sent in a problem report"
        }
    }

    @objc func sendReportPressed(sender: UIButton) {
        activitySpinner.startAnimating()
        sendButton.isDisabled = true
        viewLogButton.isDisabled = true
        
        DispatchQueue.global().async {
            let success = CrashReportService.sendManualProblemReport()

            DispatchQueue.main.async {
                if success {
                    AppDelegate.rootView?.makeToast("Problem report sent successfully")
                } else {
                    AppDelegate.rootView?.makeToast("Failed to send problem report")
                }

                // Don't re-enable the send button as there SHOULDN'T need to be a reason to send yet another report
                self.viewLogButton.isDisabled = false
                self.activitySpinner.stopAnimating()
            }
        }
    }
    
    @objc func logViewPressed(sender: UIButton) {
        let logViewController = LogViewController()
        logViewController.modalPresentationStyle = .pageSheet
        
        // Have to wrap in a nav controller or else you don't have a navigation bar and it looks pretty weird
        let vc = UINavigationController(rootViewController: logViewController)
        self.present(vc, animated: true)
    }
}

extension Int {
    // So stupid that this is even necessary. Shouldn't an INT be usable anywhere a fucking NSNumber is usable??
    func toNSNumber() -> NSNumber {
        return NSNumber(value: self)
    }
    
    func spelledOut() -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .spellOut
        
        // Common English rules to only spell out smaller numbers. Following those here b/c I'm not a savage
        if self < 10 {
            return formatter.string(from: self.toNSNumber())!
        } else {
            return self.toString()
        }
    }
}

fileprivate extension Date {
    func toTimeAgoString() -> String {
        let hoursSinceReport = Calendar.current.dateComponents([.hour], from: self, to: Date()).hour!
        
        var timeAgo = ""
        if hoursSinceReport < 1 {
            timeAgo = "less than one hour"
        } else if hoursSinceReport < 24 {
            timeAgo = "\(hoursSinceReport.spelledOut()) hours"
        } else {
            let daysAgo = Int(hoursSinceReport / 24)
            
            if daysAgo < 30 {
                timeAgo = "\(daysAgo.spelledOut()) day\(daysAgo == 1 ? "" : "s")"
            } else {
                timeAgo = "more than a month"
            }
        }
        
        return "\(timeAgo) ago"
    }
}

class GGButton : UIButton {
    var isDisabled: Bool = false {
        didSet {
            self.isEnabled = !isDisabled
            self.alpha = self.isEnabled ? 1.0 : 0.4
        }
    }
}
