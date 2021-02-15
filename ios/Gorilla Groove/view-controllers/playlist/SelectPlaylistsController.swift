import Foundation
import UIKit

class SelectPlaylistsController : UIViewController, UITableViewDataSource, UITableViewDelegate {
    
    private var playlists: [Playlist] = []
    private var selectedPlaylistIds = Set<Int>()
    private let tracks: [Track]
    
    private let tableView = UITableView()
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.color = Colors.foreground
        spinner.hidesWhenStopped = true
        
        return spinner
    }()
    
    private lazy var sendButton = UIBarButtonItem(title: "Add", style: .plain, action: { [weak self] in self?.sendAddRequest() })

    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Add to Playlist"
        
        self.view.addSubview(tableView)
        self.view.addSubview(activitySpinner)

        tableView.register(SelectEntityCell<Playlist>.self, forCellReuseIdentifier: "userCell")

        tableView.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            activitySpinner.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: -23),
            activitySpinner.topAnchor.constraint(equalTo: self.view.topAnchor, constant: 10),
        ])
        
        tableView.dataSource = self
        tableView.delegate = self
        
        var newNavItems = self.navigationItem.rightBarButtonItems ?? []
        newNavItems.insert(sendButton, at: 0)
        self.navigationItem.rightBarButtonItems = newNavItems
        
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        GGNavLog.info("Loaded select playlists controller")
        
        playlists = PlaylistDao.getPlaylists()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return playlists.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "userCell", for: indexPath) as! SelectEntityCell<Playlist>
        
        cell.entity = playlists[indexPath.row]
        cell.isActive = selectedPlaylistIds.contains(cell.entity!.id)

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! SelectEntityCell<Playlist>
        
        cell.animateSelectionColor()
        
        let playlist = cell.entity!
        
        if cell.isActive {
            selectedPlaylistIds.remove(playlist.id)
        } else {
            selectedPlaylistIds.insert(playlist.id)
        }
        
        cell.isActive = !cell.isActive
    }
    
    func sendAddRequest() {
        // Prevent double taps
        if activitySpinner.isAnimating { return }
        
        if selectedPlaylistIds.isEmpty { return }
        
        activitySpinner.startAnimating()
        // Hide the button by changing the text color. If we just remove it, then the filter option will shift weirdly
        sendButton.tintColor = Colors.navigationBackground
        
        let request = AddToPlaylistRequest(trackIds: tracks.map { $0.id }, playlistIds: selectedPlaylistIds.toArray())
        let this = self
        let pluralWord = selectedPlaylistIds.count == 1 ? "playlist" : "playlists"
        HttpRequester.post("playlist/track", AddToPlaylistResponse.self, request) { res, status, _ in
            guard let playlistTracks = res?.items, status.isSuccessful() else {
                DispatchQueue.main.async {
                    Toast.show("Failed to add track to \(pluralWord)", view: this.view)
                    this.sendButton.tintColor = Colors.primary
                    this.activitySpinner.stopAnimating()
                }
                return
            }
            
            playlistTracks.forEach { playlistTrack in
                PlaylistTrackDao.save(playlistTrack.asEntity() as! PlaylistTrack)
            }
            
            DispatchQueue.main.async {
                this.navigationController!.dismiss(animated: true)
                Toast.show("Track added to \(pluralWord)")
            }
        }
    }

    init(_ tracks: [Track]) {
        self.tracks = tracks
        
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

struct AddToPlaylistRequest : Codable {
    let trackIds: [Int]
    let playlistIds: [Int]
}

struct AddToPlaylistResponse : Codable {
    let items: [PlaylistTrackResponse]
}
