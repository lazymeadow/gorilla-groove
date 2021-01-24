import Foundation
import UIKit
import AVKit

class AddFromSpotifyController : UIViewController {
        
    private let submitButton: GGButton = {
        let button = GGButton()
        button.setTitle("Search Artist", for: .normal)
        
        return button
    }()
    
    private lazy var inputField: AutoCompleteInputField = {
        let field = AutoCompleteInputField()
        field.textField.font = field.textField.font!.withSize(22)
        
        let placeholderText = "Spotify Artist"
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
    
    static let title = "Search Spotify"

    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = AddFromSpotifyController.title
        
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
        
        inputField.submitHandler = fetchSongsFromApi
        inputField.textChangeHandler = textChangeHandler
        submitButton.addTarget(self, action: #selector(handleButtonTap), for: .touchUpInside)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        inputField.textField.becomeFirstResponder()
        
        GGNavLog.info("Loaded AddFromSpotifyController")
    }
    
    @objc private func handleButtonTap() {
        fetchSongsFromApi(inputField.text)
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
            
            HttpRequester.get("search/autocomplete/spotify/artist-name/\(text)", AutocompleteResponse.self) { response, status, _ in
                DispatchQueue.main.async {
                    self.inputField.loading = false
                }
                
                guard let suggestions = response?.suggestions.takeFirst(5), status.isSuccessful() else {
                    GGLog.error("Failed to get autocomplete response!")
                    return
                }
                
                DispatchQueue.main.async {
                    if self.inputField.text == originalText {
                        self.inputField.autoCompleteData = suggestions
                    }
                }
            }
        }
    }
    
    struct AutocompleteResponse: Codable {
        let suggestions: [String]
    }
    
    private func fetchSongsFromApi(_ text: String) {
        if text.isEmpty {
            return
        }
        	
        self.inputField.resignFirstResponder()
        loading = true
        HttpRequester.get("search/spotify/artist/\(text)", SpotifyTrackSearchResponse.self) { response, status, _ in
            DispatchQueue.main.async {
                self.loading = false
            }
            
            guard let items = response?.items, status.isSuccessful() else {
                GGLog.error("Failed to get spotify search response for term \(text)!")
                DispatchQueue.main.async {
                    Toast.show("Data could not be loaded")
                }
                
                return
            }
            
            if items.isEmpty {
                DispatchQueue.main.async {
                    Toast.show("No songs found by \(text)")
                }
                return
            }
            
            DispatchQueue.main.async {
                let vc = AddFromSpotifyTrackListController(
                    spotifyItems: items.map { $0.toViewableTrack() },
                    searchTerm: text
                )
                vc.modalPresentationStyle = .fullScreen
                self.navigationController!.pushViewController(vc, animated: true)
            }
        }
    }
}

struct SpotifyTrackSearchResponse : Codable {
    let items: [SpotifyTrackResponse]
}

struct SpotifyTrackResponse: Codable {
    let sourceId: String
    let name: String
    let artist: String
    let album: String
    let releaseYear: Int
    let trackNumber: Int
    let albumArtLink: String?
    let length: Int
    let previewUrl: String?
    
    func toViewableTrack() -> SpotifyTrack {
        SpotifyTrack(
            sourceId: sourceId,
            name: name,
            artist: artist,
            album: album,
            releaseYear: releaseYear,
            trackNumber: trackNumber,
            albumArtLink: albumArtLink,
            length: length,
            previewUrl: previewUrl
        )
    }
}

struct SpotifyTrack: ViewableTrackData {
    var id: Int = -1
    
    var artistString: String {
        get {
            artist
        }
    }
        
    let sourceId: String
    var name: String
    let artist: String
    var album: String
    let releaseYear: Int
    let trackNumber: Int
    let albumArtLink: String?
    var length: Int
    let previewUrl: String?
    
    func toImportRequest(addToReview: Bool, artistQueueName: String?) -> SpotifyImportRequest {
        return SpotifyImportRequest(
            name: self.name,
            artist: self.artist,
            album: self.album,
            releaseYear: self.releaseYear,
            trackNumber: self.trackNumber,
            albumArtLink: self.albumArtLink,
            length: self.length,
            previewUrl: self.previewUrl,
            addToReview: addToReview,
            artistQueueName: artistQueueName
        )
    }
}

struct SpotifyImportRequest : Codable {
    var name: String
    let artist: String
    var album: String
    let releaseYear: Int
    let trackNumber: Int
    let albumArtLink: String?
    var length: Int
    let previewUrl: String?
    let addToReview: Bool
    let artistQueueName: String?
}

class AddFromSpotifyTrackListController : UIViewController, UITableViewDataSource, UITableViewDelegate {
    
    private let tableView = UITableView()
    
    private let years: [Int]
    private var tracksByYear: [Int: [SpotifyTrack]]
    private let searchTerm: String
    
    private var playingId: Int? = nil
    
    private var player: AVPlayer = {
        // See the comment in AudioPlayer.swift for more info on why this is split out like this
        // https://stackoverflow.com/a/57829394/13175115
        AVPlayer()
    }()
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = true
        spinner.color = Colors.foreground
        
        return spinner
    }()
    
    fileprivate init(spotifyItems: [SpotifyTrack], searchTerm: String) {
        var spotifyItems = spotifyItems
        // Assign a temporary ID to the items so we can highlight the playing row (and maybe other stuff later)
        for i in 0..<spotifyItems.count {
            spotifyItems[i].id = i
        }
        
        tracksByYear = spotifyItems.groupBy { $0.releaseYear }
        years = tracksByYear.keys.sorted { $0 > $1 }
        self.searchTerm = searchTerm

        super.init(nibName: nil, bundle: nil)
        
        player.automaticallyWaitsToMinimizeStalling = true
        player.volume = 1.0
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(tableView)

        tableView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(TrackViewCell.self, forCellReuseIdentifier: "addFromSpotify")
        
        tableView.tableFooterView = UIView(frame: .zero)
        
        AudioPlayer.observePlaybackChanged(self) { vc, isPlaying in
            if isPlaying {
                DispatchQueue.main.async {
                    vc.player.pause()
                }
            }
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        GGNavLog.info("Loaded add from spotify track list controller")
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return years[section].toString()
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return years.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let year = years[section]
        return tracksByYear[year]?.count ?? 0
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "addFromSpotify", for: indexPath) as! TrackViewCell
        let year = years[indexPath.section]
        let track = tracksByYear[year]![indexPath.row]
        
        cell.track = track
        cell.checkIfPlaying(idToCheckAgainst: playingId)
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
            
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! TrackViewCell
        cell.animateSelectionColor()
        
        let track = cell.track as! SpotifyTrack

        GGNavLog.info("User tapped on cell for track: \(track.artistString) - \(track.name)")
        
        showEditMenu(track)
    }
    
    private func showEditMenu(_ track: SpotifyTrack) {
        GGNavLog.info("User tapped a review source")
        
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        let title = track.previewUrl == nil ? "No Preview Available" : "Play Preview"
        alert.addAction(UIAlertAction(title: title, style: .default, handler: { (_) in
            GGNavLog.info("User tapped play preview on '\(track.artist) - \(track.name)'")
            
            if let urlStr = track.previewUrl, let url = URL(string: urlStr) {
                AudioPlayer.pause()
                
                // FIXME
                // Somehow this screws up the notification center's play button despite the fact that the playback rate seems to be 0.0
                // Not a big deal. Just requires the user to tap pause then play if they previewed music while listening to other music.
                // Might be an easy fix, but in an ideal world there is only one AVAudioPlayer for the entire app. Will have to
                // decouple the AudioPlayer from being so closely aligned to a Track
                let playerItem = AVPlayerItem(url: url)
                self.player.replaceCurrentItem(with: playerItem)
                self.player.play()
                
                self.playingId = track.id
                
                DispatchQueue.main.async {
                    self.tableView.visibleCells.forEach { cell in
                        (cell as! TrackViewCell).checkIfPlaying(idToCheckAgainst: self.playingId)
                    }
                }
            }
        }))
        alert.addAction(UIAlertAction(title: "Import to Library", style: .default, handler: { _ in
            GGNavLog.info("User tapped import to library on '\(track.artist) - \(track.name)'")
            
            self.importToLibrary(track: track, addToReview: false)
        }))
        alert.addAction(UIAlertAction(title: "Import to Review Queue", style: .default, handler: { _ in
            GGNavLog.info("User tapped import to review queue on '\(track.artist) - \(track.name)'")
            
            self.importToLibrary(track: track, addToReview: true, artistQueueName: self.searchTerm)
        }))
        alert.addAction(UIAlertAction(title: "Cancel", style: .default, handler: { (_) in
            GGNavLog.info("User tapped cancel button")
        }))
        
        ViewUtil.showAlert(alert)
    }
    
    private func importToLibrary(track: SpotifyTrack, addToReview: Bool, artistQueueName: String? = nil) {
        self.activitySpinner.startAnimating()
        
        let request = track.toImportRequest(addToReview: addToReview, artistQueueName: artistQueueName)
        
        HttpRequester.post("background-task/metadata-dl", BackgroundTaskResponse.self, request) { response, status, _ in
            guard let taskResponse = response, status.isSuccessful() else {
                DispatchQueue.main.async {
                    Toast.show("Failed to start import")
                    self.activitySpinner.stopAnimating()
                }
                GGLog.error("Could not start spotify import!")
                return
            }
            
            DispatchQueue.main.async {
                Toast.show("Import started")
                self.activitySpinner.stopAnimating()
            }
            
            BackgroundTaskService.addBackgroundTasks(taskResponse.items)
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
