import UIKit
import Foundation

class ArtistViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    private var artists: [String] = []
    private var visibleArtists: [String] = []
    private var tracks: [Track] = []
    private let originalView: LibraryViewType?
    private let user: User?
    
    private var lastOfflineMode: Bool

    private lazy var filterOptions: [[FilterOption]] = [
        MyLibraryHelper.getNavigationOptions(vc: self, viewType: .ARTIST, user: user),
    ]
    
    private lazy var filter = TableFilter(filterOptions, vc: self)
    
    private let tableView = UITableView()
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = true
        spinner.color = Colors.foreground
        
        return spinner
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        GGNavLog.info("Loaded artist view")
        
        view.addSubview(tableView)
        view.addSubview(activitySpinner)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo:view.topAnchor),
            tableView.leftAnchor.constraint(equalTo:view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo:view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo:view.bottomAnchor),
            
            activitySpinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            activitySpinner.centerYAnchor.constraint(equalTo: view.safeAreaLayoutGuide.centerYAnchor),
            
            filter.topAnchor.constraint(equalTo: view.topAnchor),
            filter.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -10),
        ])
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(ArtistViewCell.self, forCellReuseIdentifier: "artistCell")
        
        TableSearchAugmenter.addSearchToNavigation(
            controller: self,
            tableView: tableView,
            onTap: { [weak self] in self?.filter.setIsHiddenAnimated(true) }
        ) { [weak self] input in
            guard let this = self else { return }
            
            let searchTerm = input.lowercased()
            if (searchTerm.isEmpty) {
                this.visibleArtists = this.artists
            } else {
                this.visibleArtists = this.artists.filter { $0.lowercased().contains(searchTerm) }
            }
        }
        
        // Remove extra table rows when we don't have a full screen of songs
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // If it wasn't empty, it means we re-loaded this already existing view and it shouldn't load more stuff unless we know stuff changed
        if !tracks.isEmpty && lastOfflineMode == OfflineStorageService.offlineModeEnabled {
            return
        }
        
        lastOfflineMode = OfflineStorageService.offlineModeEnabled
        activitySpinner.startAnimating()

        if user != nil {
            loadWebArtists()
        } else {
            DispatchQueue.global().async { [weak self] in
                guard let this = self else { return }
                
                this.tracks = TrackService.getTracks()
                this.reloadArtists()
            }
        }
    }
    
    private func loadWebArtists() {
        let url = "track?userId=\(user!.id)&sort=name,ASC&size=100000&page=0&showHidden=true"
        HttpRequester.get(url, LiveTrackRequest.self) { [weak self] res, status, _ in
            guard let this = self else { return }
            guard let trackResponse = res?.content, status.isSuccessful() else {
                this.artists = []
                this.visibleArtists = []
                
                DispatchQueue.main.async {
                    Toast.show("Could not load artists")
                    this.tableView.reloadData()
                }
                return
            }
            
            this.tracks = trackResponse.map { $0.asTrack(userId: this.user!.id) }
            this.reloadArtists()
        }
    }
    
    private func reloadArtists() {
        artists = TrackService.getArtistsFromTracks(tracks)
        visibleArtists = artists
        
        DispatchQueue.main.async { [weak self] in
            self?.activitySpinner.stopAnimating()
            self?.tableView.reloadData()
        }
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleArtists.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "artistCell", for: indexPath) as! ArtistViewCell
        let artist = visibleArtists[indexPath.row]
        
        cell.artist = artist
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        filter.setIsHiddenAnimated(true)
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! ArtistViewCell
        
        cell.animateSelectionColor()
        filter.setIsHiddenAnimated(true)

        let artist = cell.artist!
        
        let albums = TrackService.getAlbumsFromTracks(tracks, artist: artist)
        
        // If we only have one album to view, may as well just load it and save ourselves a tap
        let view: UIViewController = {
            if (albums.count == 1) {
                return TrackViewController(albums.first!.name, originalView: .ARTIST, artistFilter: artist, albumFilter: albums.first!.name, user: user)
            } else {
                return AlbumViewController(cell.nameLabel.text!, albums, cell.artist!, user: user)
            }
        }()
        
        navigationController!.pushViewController(view, animated: true)
    }
    
    init(_ title: String, originalView: LibraryViewType? = nil, user: User? = nil) {
        self.originalView = originalView
        self.user = user
        self.lastOfflineMode = OfflineStorageService.offlineModeEnabled

        super.init(nibName: nil, bundle: nil)

        self.title = title
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
