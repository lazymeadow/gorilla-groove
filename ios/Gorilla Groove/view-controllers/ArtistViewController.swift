import UIKit
import Foundation

class ArtistViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    private var artists: [String] = []
    private var visibleArtists: [String] = []
    private var tracks: [Track] = []
    private let originalView: LibraryViewType?
    private let user: User?
    
    private var showHiddenTracks: Bool
    private var lastOfflineMode: Bool

    private lazy var filterOptions: [[FilterOption]] = [
        MyLibraryHelper.getNavigationOptions(vc: self, viewType: .ARTIST, user: user),
        [
            FilterOption("Show Hidden Tracks", filterImage: showHiddenTracks ? .CHECKED : .NONE) { [weak self] option in
                guard let this = self else { return }
                this.showHiddenTracks = !this.showHiddenTracks
                option.filterImage = this.showHiddenTracks ? .CHECKED : .NONE
                this.filter.reloadData()
                this.refreshArtists()
            },
        ]
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
        ) { [weak self] _ in
            self?.setVisibleArtists()
        }
        
        // If offline mode changes while we're actively looking at this VC, then we should update it.
        // Otherwise, it will update when the user loads the view later.
        SettingsService.observeOfflineModeChanged(self) { _, offlineModeEnabled in
            DispatchQueue.main.async { [weak self] in
                if self?.isActiveVc == true {
                    self?.lastOfflineMode = OfflineStorageService.offlineModeEnabled
                    self?.loadTracks()
                }
            }
        }
        
        // Remove extra table rows when we don't have a full screen of songs
        tableView.tableFooterView = UIView(frame: .zero)
        
        // Because the footer has no size, set an additional handler on the controller's view to make sure tapping on empty space closes it
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(closeFilter))
        view.addGestureRecognizer(tapGesture)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        GGNavLog.info("Loaded artist view")

        // If it wasn't empty, it means we re-loaded this already existing view and it shouldn't load more stuff unless we know stuff changed
        if !artists.isEmpty && lastOfflineMode == OfflineStorageService.offlineModeEnabled {
            return
        }
        
        activitySpinner.startAnimating()
        
        if tracks.isEmpty || lastOfflineMode != OfflineStorageService.offlineModeEnabled {
            loadTracks()
        } else {
            refreshArtists()
        }
    }
    
    private func loadTracks() {
        if user != nil {
            loadWebTracks()
        } else {
            DispatchQueue.global().async { [weak self] in
                guard let this = self else { return }
                
                // Always get hidden tracks (aka nil which gets both). We'll filter them out later if we don't want to see them.
                // This prevents us from having to reload tracks if we want to toggle them on or off
                this.tracks = TrackService.getTracks(showHidden: nil)
                DispatchQueue.main.async {
                    this.refreshArtists()
                }
            }
        }
    }
    
    private func setVisibleArtists() {
        let searchTerm = searchText.lowercased()
        if (searchTerm.isEmpty) {
            visibleArtists = artists
        } else {
            visibleArtists = artists.filter { $0.lowercased().contains(searchTerm) }
        }
    }
    
    @objc private func closeFilter(sender: UITapGestureRecognizer) {
        filter.setIsHiddenAnimated(true)
    }
    
    private func loadWebTracks() {
        let url = "track?userId=\(user!.id)&sort=name,ASC&size=100000&page=0&showHidden=true"
        HttpRequester.get(url, LiveTrackRequest.self) { [weak self] res, status, _ in
            guard let this = self else { return }
            guard let trackResponse = res?.content, status.isSuccessful() else {
                this.artists = []
                this.visibleArtists = []
                this.tracks = []
                
                DispatchQueue.main.async {
                    Toast.show("Could not load artists")
                    this.tableView.reloadData()
                }
                return
            }
            
            this.tracks = trackResponse.map { $0.asTrack(userId: this.user!.id) }
            DispatchQueue.main.async {
                this.refreshArtists()
            }
        }
    }
    
    private func refreshArtists() {
        artists = TrackService.getArtistsFromTracks(tracks, showHidden: showHiddenTracks)
        setVisibleArtists()
        
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
        
        let albums = TrackService.getAlbumsFromTracks(tracks, artist: artist, showHidden: showHiddenTracks)
        
        // If we only have one album to view, may as well just load it and save ourselves a tap
        let view: UIViewController = {
            if (albums.count == 1) {
                return TrackViewController(albums.first!.name, originalView: .ARTIST, artistFilter: artist, albumFilter: albums.first!.name, user: user, showingHidden: showHiddenTracks)
            } else {
                return AlbumViewController(cell.nameLabel.text!, albums, tracks, cell.artist!, user: user, showingHidden: showHiddenTracks)
            }
        }()
        
        navigationController!.pushViewController(view, animated: true)
    }
    
    init(
        _ title: String,
        originalView: LibraryViewType? = nil,
        user: User? = nil,
        showingHidden: Bool? = nil
    ) {
        self.originalView = originalView
        self.user = user
        self.lastOfflineMode = OfflineStorageService.offlineModeEnabled
        self.showHiddenTracks = showingHidden ?? (user != nil)
        
        super.init(nibName: nil, bundle: nil)

        self.title = title
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
