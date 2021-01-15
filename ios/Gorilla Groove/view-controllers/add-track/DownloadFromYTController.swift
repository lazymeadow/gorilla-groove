import Foundation
import UIKit

class DownloadFromYTController : UIViewController {
    private let submitButton: GGButton = {
        let button = GGButton()
        button.setTitle("Download", for: .normal)
        
        return button
    }()
    
    private lazy var inputField: UITextField = {
        let field = UITextField()
        field.font = field.font!.withSize(22)
        
        let placeholderText = "YouTube Video URL"
        field.attributedPlaceholder = NSAttributedString(
            string: placeholderText,
            attributes: [NSAttributedString.Key.foregroundColor : Colors.inputLine]
        )
        
        field.autocorrectionType = .no
        field.translatesAutoresizingMaskIntoConstraints = false
        
        return field
    }()
    
    private let inputCaption: UILabel = {
        let label = UILabel()
        label.font = label.font.withSize(11)
        label.textColor = Colors.inputLine
        label.translatesAutoresizingMaskIntoConstraints = false
        
        return label
    }()
    
    private let bottomInputLine: UIView = {
        let view = UIView()
        
        view.backgroundColor = Colors.inputLine
        view.translatesAutoresizingMaskIntoConstraints = false
        
        return view
    }()
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = true
        spinner.color = Colors.foreground
        
        return spinner
    }()
    
    private var loading = false {
        didSet {
            if loading {
                activitySpinner.startAnimating()
            } else {
                activitySpinner.stopAnimating()
            }
            
            submitButton.isDisabled = loading
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Download from YouTube"
        
        inputCaption.text = "Playlist downloads are not supported"
        
        self.view.backgroundColor = Colors.background
        
        self.view.addSubview(submitButton)
        self.view.addSubview(bottomInputLine)
        self.view.addSubview(inputCaption)
        self.view.addSubview(inputField)
        self.view.addSubview(activitySpinner)
        
        NSLayoutConstraint.activate([
            inputField.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: -45),
            inputField.leadingAnchor.constraint(equalTo: self.view.leadingAnchor, constant: 45),
            inputField.topAnchor.constraint(equalTo: self.view.topAnchor, constant: 50),
            
            bottomInputLine.trailingAnchor.constraint(equalTo: inputField.trailingAnchor, constant: 16),
            bottomInputLine.leadingAnchor.constraint(equalTo: inputField.leadingAnchor, constant: -16),
            bottomInputLine.heightAnchor.constraint(equalToConstant: 1),
            bottomInputLine.topAnchor.constraint(equalTo: inputField.bottomAnchor, constant: 5),
            
            inputCaption.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
            inputCaption.leadingAnchor.constraint(equalTo: inputField.leadingAnchor, constant: 0),
            inputCaption.topAnchor.constraint(equalTo: bottomInputLine.topAnchor, constant: 10),
            
            submitButton.leadingAnchor.constraint(equalTo: bottomInputLine.leadingAnchor),
            submitButton.trailingAnchor.constraint(equalTo: bottomInputLine.trailingAnchor),
            submitButton.heightAnchor.constraint(equalToConstant: 50),
            submitButton.topAnchor.constraint(equalTo: inputCaption.bottomAnchor, constant: 75),
            
            activitySpinner.centerXAnchor.constraint(equalTo: bottomInputLine.centerXAnchor),
            activitySpinner.bottomAnchor.constraint(equalTo: submitButton.topAnchor, constant: -10),
        ])
        
        inputField.addTarget(self, action: #selector(handleSubmit), for: .primaryActionTriggered)
        submitButton.addTarget(self, action: #selector(handleSubmit), for: .touchUpInside)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        inputField.becomeFirstResponder()
        
        GGNavLog.info("Loaded DownloadFromYTController")
    }
    
    @objc private func handleSubmit() {
        postToApi(inputField.text ?? "")
    }
    
    private func postToApi(_ text: String) {
        if text.isEmpty {
            return
        }
        
        loading = true
        GGLog.info("Requesting download of YouTube video with URL: \(text)")
        
        let request = DownloadYTVideoRequest(url: text)
        
        HttpRequester.post("track/youtube-dl", TrackResponse.self, request) { response, status, _ in
            guard let newTrack = response?.asTrack(), status.isSuccessful() else {
                DispatchQueue.main.async {
                    Toast.show("Failed to download video")
                    self.loading = false
                }
                GGLog.error("Could not download video!")
                return
            }
            
            TrackDao.save(newTrack)
            
            DispatchQueue.global().async {
                ServerSynchronizer.syncWithServer(syncTypes: [SyncType.track])
            }
            
            DispatchQueue.main.async {
                Toast.show("Downloaded video")
                self.loading = false

                self.navigationController!.popViewController(animated: true)
            }
        }
    }
    
    struct DownloadYTVideoRequest: Codable {
        let url: String
    }
}
