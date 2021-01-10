import Foundation
import UIKit

class AddReviewSourcesController : UIViewController {
    
    private let sourceType: SourceType
    private let editReviewController: EditReviewSourcesController
    
    private let submitButton: GGButton = {
        let button = GGButton()
        button.setTitle("Queue it up", for: .normal)
        
        return button
    }()
    
    private lazy var inputField: AutoCompleteInputField = {
        let field = AutoCompleteInputField()
        field.textField.font = field.textField.font!.withSize(22)
        
        let placeholderText = sourceType == .ARTIST ? "Spotify Artist" : "Channel Name or URL"
        field.textField.attributedPlaceholder = NSAttributedString(
            string: placeholderText,
            attributes: [NSAttributedString.Key.foregroundColor : Colors.inputLine]
        )
        
        field.textField.autocorrectionType = .no
        
        return field
    }()
    
    private let inputCaption: UILabel = {
        let label = UILabel()
        label.font = label.font.withSize(11)
        label.textColor = Colors.inputLine
        label.text = "Artist Name"
        label.sizeToFit()
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
    
    init(
        sourceType: SourceType,
        editReviewController: EditReviewSourcesController
    ) {
        self.sourceType = sourceType
        self.editReviewController = editReviewController
        
        super.init(nibName: nil, bundle: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        if sourceType == .ARTIST {
            self.title = "Add Artist Queue"
            inputCaption.text = "Uploads to Spotify will be added to your review queue"
        } else if sourceType == .YOUTUBE_CHANNEL {
            self.title = "Add YouTube Queue"
            inputCaption.text = "Videos over 10 minutes will not be added to your queue"
        }
        
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
            bottomInputLine.topAnchor.constraint(equalTo: inputField.textField.bottomAnchor, constant: 5),
            
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
        
        inputField.submitHandler = postToApi
        inputField.textChangeHandler = textChangeHandler
        submitButton.addTarget(self, action: #selector(handleButtonTap), for: .touchUpInside)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        inputField.textField.becomeFirstResponder()
    }
    
    @objc private func handleButtonTap() {
        postToApi(inputField.text)
    }
    
    private func textChangeHandler(_ text: String) {
        inputField.autoCompleteData = []
        
        // Don't want to spam the endpoint as someone is actively typing. Put in a debounce so it doesn't fire immediately
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.40) {
            DispatchQueue.main.async {
                self.textChangeHandlerDebounced(text)
            }
        }
    }
    
    private func textChangeHandlerDebounced(_ originalText: String) {
        let text = self.inputField.text
        if text != originalText {
            return
        }
        
        // If the request is already being sent, don't bother populating anything
        if loading {
            return
        }
               
        // No point in searching a single character is there
        if text.count > 1 {
            GGLog.debug("Requesting autocomplete info for text \(text)")
            
            inputField.loading = true
            let urlPart = sourceType == .ARTIST ? "spotify/artist-name" : "youtube/channel-name"
            
            HttpRequester.get("search/autocomplete/\(urlPart)/\(text)", AutocompleteResponse.self) { response, status, _ in
                DispatchQueue.main.async {
                    self.inputField.loading = false
                }

                guard let suggestions = response?.suggestions.takeFirst(5), status.isSuccessful() else {
                    GGLog.error("Failed to get autocomplete response!")
                    return
                }
                
                DispatchQueue.main.async {
                    self.inputField.autoCompleteData = suggestions
                }
            }
        } else {
            inputField.loading = false
        }
    }
    
    struct AutocompleteResponse: Codable {
        let suggestions: [String]
    }
    
    private func postToApi(_ text: String) {
        if text.isEmpty {
            return
        }
        
        loading = true
        GGLog.info("Posting new \(sourceType) source to API with value \(inputField.text)")
        
        // Don't need to keep the suggestions open if we're already sending a request
        self.inputField.autoCompleteData = []
        
        if sourceType == .ARTIST {
            let request = AddArtistSourceRequest(artistName: text)
            
            HttpRequester.post("review-queue/subscribe/artist", ReviewSourceResponse.self, request) { response, status, _ in
                self.handleAddResponse(response: response, status: status, text: text)
            }
        } else if sourceType == .YOUTUBE_CHANNEL {
            let isUrl = text.starts(with: "https:")
            let request = AddYoutubeChannelRequest(
                channelUrl: isUrl ? text : nil,
                channelTitle: isUrl ? nil : text
            )
            HttpRequester.post("review-queue/subscribe/youtube-channel", ReviewSourceResponse.self, request) { response, status, _ in
                self.handleAddResponse(response: response, status: status, text: text)
            }
        } else {
            GGLog.critical("Attempted to add unrecognized source type \(sourceType)!")
        }
    }
    
    private func handleAddResponse(response: ReviewSourceResponse?, status: Int, text: String) {
        guard let newSource = response?.asEntity() as? ReviewSource, status.isSuccessful() else {
            DispatchQueue.main.async {
                Toast.show("Failed to subscribe to the \(self.sourceType == .ARTIST ? "artist" : "YouTube channel")")
                self.loading = false
            }
            GGLog.error("Could not create new review source!")
            return
        }
        
        // This gets a little weird, but we add the review source because why not, right? We know we need it
        ReviewSourceDao.save(newSource)
        
        // This is kind of a needless optimization to keep the ReviewQueueController from updating itself unnecessarily,
        // since adding the source to the EditReviewController will tell the ReviewQueueController that it needs to refresh,
        // and so will a sync. It doesn't really matter much if it actually succeeds or anything, though.
        DispatchQueue.global().async {
            ServerSynchronizer.syncWithServer(syncTypes: [SyncType.reviewSource])
        }
        
        DispatchQueue.main.async {
            Toast.show("Subscribed to \(text)")
            self.loading = false

            self.editReviewController.addSource(newSource)
            self.navigationController!.popViewController(animated: true)
        }
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    struct AddArtistSourceRequest: Codable {
        let artistName: String
    }
    
    struct AddYoutubeChannelRequest: Codable {
        let channelUrl: String?
        let channelTitle: String?
    }
}
