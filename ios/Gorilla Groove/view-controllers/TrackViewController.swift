import UIKit
import Foundation

class TrackViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    
    private var loadTracksFunc: (() -> [Track])? = nil
    private var trackIds: [Int] = []
    private var trackIdToTrack: [Int: Track] = [:]
    private var visibleTrackIds: [Int] = []
    private let scrollPlayedTrackIntoView: Bool
    private let showingHidden: Bool
    private let tableView = UITableView()
    private let artistFilter: String?
    private let albumFilter: String?
    private var sortOverrideKey: String? = nil
    private var sortDirectionAscending: Bool = true
    private let user: User?
    private let alwaysReload: Bool
    
    private var lastOfflineMode: Bool
    
    private let originalView: LibraryViewType
    
    private let sortsIndex = 1
    
    // This never gets invoked. I've got a retain cycle somewhere but the memory graph in xcode crashes the shitty program
    // so debugging where this is has been proving to be a challenge.
    deinit {
        GGLog.info("Deinit TVC")
    }
    
    private lazy var filterOptions: [[FilterOption]] = {
        if originalView == .PLAYLIST || originalView == .NOW_PLAYING {
            return []
        }
        
        let albumSort = originalView == .ARTIST || originalView == .ALBUM
        
        let options = [
            MyLibraryHelper.getNavigationOptions(vc: self, viewType: originalView, user: user),
            [
                FilterOption("Sort by Name", filterImage: albumSort ? .NONE : .ARROW_UP) { [weak self] option in
                    guard let this = self else { return }
                    this.handleSortChange(option: option, key: this.getSortKey("name"), initialSortAsc: true)
                },
                FilterOption("Sort by Play Count") { [weak self] option in
                    guard let this = self else { return }
                    this.handleSortChange(option: option, key: this.getSortKey("play_count"), initialSortAsc: false)
                },
                FilterOption("Sort by Date Added") { [weak self] option in
                    guard let this = self else { return }
                    this.handleSortChange(option: option, key: this.getSortKey("added_to_library"), initialSortAsc: false)
                },
                FilterOption("Sort by Album", filterImage: albumSort ? .ARROW_UP : .NONE) { [weak self] option in
                    guard let this = self else { return }
                    this.handleSortChange(option: option, key: this.getSortKey("album"), initialSortAsc: true)
                },
            ]
        ]
        
        sortOverrideKey = albumSort ? "album" : "name"

        return options
    }()
    
    // Web uses different sort keys than the app's DB
    func getSortKey(_ key: String) -> String {
        switch (key) {
        case "name": return key
        case "play_count": return user == nil ? key : "playCount"
        case "added_to_library": return user == nil ? key : "addedToLibrary"
        case "album": return user == nil ? key : "addedToLibrary"
        default:
            fatalError("Unsupported sort key: \(key)")
        }
    }
    
    private lazy var filter: TableFilter? = {
        if !filterOptions.isEmpty {
            return TableFilter(filterOptions, vc: self)
        } else {
            return nil
        }
    }()
    
    private func handleSortChange(option: FilterOption, key: String, initialSortAsc: Bool) {
        // This [1] is so dumb don't hate me
        filterOptions[1].forEach { $0.filterImage = .NONE }
        
        if sortOverrideKey == key {
            sortDirectionAscending = !sortDirectionAscending
        } else {
            sortDirectionAscending = initialSortAsc
            sortOverrideKey = key
        }
        option.filterImage = sortDirectionAscending ? .ARROW_UP : .ARROW_DOWN
        
        filter!.reloadData()
        loadTracks()
    }
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = true
        spinner.color = Colors.foreground
        
        return spinner
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        GGNavLog.info("Loaded track view")

        view.addSubview(tableView)
        view.addSubview(activitySpinner)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            activitySpinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            activitySpinner.centerYAnchor.constraint(equalTo: view.safeAreaLayoutGuide.centerYAnchor),
        ])
        filter?.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        filter?.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -10).isActive = true
        
        tableView.keyboardDismissMode = .onDrag
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(TrackViewCell.self, forCellReuseIdentifier: "songCell")
        
        // Remove extra table rows when we don't have a full screen of songs
        // Might possibly use this to display warnings for like, offline mode or w/e later
        let footerView = UIView(frame: CGRect.init(x: 0, y: 0, width: tableView.frame.width, height: 0))
        footerView.backgroundColor = UIColor.green
        tableView.tableFooterView = footerView
        
        TableSearchAugmenter.addSearchToNavigation(
            controller: self,
            tableView: tableView,
            onTap: { [weak self] in self?.filter?.setIsHiddenAnimated(true) }
        ) { [weak self] input in
            guard let this = self else { return }
            
            let searchTerm = input.lowercased()
            if (searchTerm.isEmpty) {
                this.visibleTrackIds = this.trackIds
            } else {
                this.visibleTrackIds = this.trackIds.filter {
                    let track = this.trackIdToTrack[$0]!
                    return track.name.lowercased().contains(searchTerm)
                }
            }
        }
        
        NowPlayingTracks.addTrackChangeObserver(self) { _, _ in
            DispatchQueue.main.async { [weak self] in
                self?.tableView.visibleCells.forEach { cell in
                    let songViewCell = cell as! TrackViewCell
                    songViewCell.checkIfPlaying()
                }
            }
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        if visibleTrackIds.isEmpty || alwaysReload || lastOfflineMode != OfflineStorageService.offlineModeEnabled {
            lastOfflineMode = OfflineStorageService.offlineModeEnabled
            loadTracks()
        }
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleTrackIds.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "songCell", for: indexPath) as! TrackViewCell
        let trackId = visibleTrackIds[indexPath.row]
        let track = trackIdToTrack[trackId]!
        
        cell.track = track
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
        
        let longPressGesture = UILongPressGestureRecognizer(target: self, action: #selector(bringUpSongContextMenu))
        cell.addGestureRecognizer(longPressGesture)
            
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let songViewCell = cell as! TrackViewCell
        songViewCell.checkIfPlaying()
    }
    
    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        filter?.setIsHiddenAnimated(true)
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        filter?.setIsHiddenAnimated(true)
        
        let cell = sender.view as! TrackViewCell
        
        let tableIndex = tableView.indexPath(for: cell)!
        
        cell.animateSelectionColor()
        let visibleTracks = visibleTrackIds.map { trackIdToTrack[$0]! }
        NowPlayingTracks.setNowPlayingTracks(visibleTracks, playFromIndex: tableIndex.row)
        
        if let search = navigationItem.searchController {
            search.searchBar.delegate!.searchBarTextDidEndEditing!(search.searchBar)
        }
    }
    
    @objc private func bringUpSongContextMenu(sender: UITapGestureRecognizer) {
        if sender.state != .began {
            return
        }
        
        let cell = sender.view as! TrackViewCell
        let tableIndex = tableView.indexPath(for: cell)!
        let trackId = visibleTrackIds[tableIndex.row]
        let track = trackIdToTrack[trackId]!
        
        let alert = TrackContextMenu.createMenuForTrack(track, parentVc: self) { [weak self] newTrack in
            guard let this = self else { return }
            if newTrack == nil || (!this.showingHidden && newTrack!.isHidden) {
                GGLog.info("Hiding existing track from menu list in response to edit")
                this.visibleTrackIds.remove(at: tableIndex.row)
                DispatchQueue.main.async {
                    this.tableView.deleteRows(at: [tableIndex], with: .automatic)
                }
            }
        }
        
        ViewUtil.showAlert(alert)
    }
    
    // TBH I don't really like how I've done this. Now that this can take either DB or Web tracks, I'd rather rework this so that
    // the tracks are loaded in from either source, and then sorted by this controller
    func loadTracks() {
        activitySpinner.startAnimating()
        
        if user == nil {
            loadDbTracks()
        } else {
            loadWebTracks()
        }
    }
    
    private func loadDbTracks() {
        DispatchQueue.global().async { [weak self] in
            guard let this = self else { return }
            let loadFunc = this.loadTracksFunc ?? {
                TrackService.getTracks(
                    album: this.albumFilter,
                    artist: this.artistFilter,
                    sortOverrideKey: this.sortOverrideKey,
                    sortAscending: this.sortDirectionAscending
                )
            }
            
            let tracks = loadFunc()
            this.trackIds = tracks.map { $0.id }
            this.trackIdToTrack = tracks.keyBy { $0.id }
            this.visibleTrackIds = this.trackIds
            
            DispatchQueue.main.async {
                this.activitySpinner.stopAnimating()
                this.tableView.reloadData()
                
                if this.scrollPlayedTrackIntoView && NowPlayingTracks.nowPlayingIndex >= 0 {
                    let indexPath = IndexPath(row: NowPlayingTracks.nowPlayingIndex, section: 0)
                    this.tableView.scrollToRow(at: indexPath, at: .middle, animated: false)
                } else if !this.visibleTrackIds.isEmpty {
                    // Reset the view to the top after reloading
                    let indexPath = IndexPath(row: 0, section: 0)
                    this.tableView.scrollToRow(at: indexPath, at: .top, animated: false)
                }
            }
        }
    }
    
    private func loadWebTracks() {
        let sortDir = sortDirectionAscending ? "ASC" : "DESC"
        let extraSort = sortOverrideKey == "album" ? "&sort=trackNumber,ASC" : ""
        
        // Offload some processing by providing a search term if we have an artist.
        // Will dramatically reduce the number of things we need to process
        let searchTerm = artistFilter == nil ? "" : "&searchTerm=\(artistFilter!)"
        
        let url = "track?userId=\(user!.id)&sort=\(sortOverrideKey!),\(sortDir)\(extraSort)\(searchTerm)&size=100000&page=0&showHidden=true"
        HttpRequester.get(url, LiveTrackRequest.self) { [weak self] res, status, _ in
            guard let this = self else { return }
            guard var tracks = res?.content, status.isSuccessful() else {
                this.trackIds = []
                this.trackIdToTrack = [:]
                this.visibleTrackIds = []
                
                DispatchQueue.main.async {
                    Toast.show("Could not load tracks")
                    this.tableView.reloadData()
                }
                return
            }
            
            // We provide the artist search term to the API, but because the search term on the API side searches for multiple fields,
            // we need to make sure it's actually valid on our side. It will be the majority of the time
            if let artistFilter = this.artistFilter?.lowercased() {
                // If artist filter is an empty string, we specifically only want tracks without artists
                if artistFilter == "" {
                    tracks = tracks.filter { $0.artist == "" && $0.featuring == "" }
                } else {
                    tracks = tracks.filter { $0.artist.lowercased().contains(artistFilter) || $0.featuring.lowercased().contains(artistFilter) }
                }
            }
            if let albumFilter = this.albumFilter?.lowercased() {
                // If album filter is an empty string, we specifically only want tracks without albums
                if albumFilter == "" {
                    tracks = tracks.filter { $0.album == "" }
                } else {
                    tracks = tracks.filter { $0.album.lowercased().contains(albumFilter) }
                }
            }
            
            this.trackIds = tracks.map { $0.id }
            this.trackIdToTrack = tracks.map { $0.asTrack(userId: this.user?.id ?? 0) }.keyBy { $0.id }
            this.visibleTrackIds = this.trackIds
            
            DispatchQueue.main.async {
                this.activitySpinner.stopAnimating()
                this.tableView.reloadData()
                
                // Reset the view to the top after reloading
                if !this.visibleTrackIds.isEmpty { // I don't THINK this should ever be empty. But it's a crash if it is
                    let indexPath = IndexPath(row: 0, section: 0)
                    this.tableView.scrollToRow(at: indexPath, at: .top, animated: false)
                }
            }
        }
    }
    
    init(
        _ title: String,
        scrollPlayedTrackIntoView: Bool = false,
        showingHidden: Bool = false,
        originalView: LibraryViewType,
        artistFilter: String? = nil,
        albumFilter: String? = nil,
        user: User? = nil,
        alwaysReload: Bool = false,
        loadTracksFunc: (() -> [Track])? = nil
    ) {
        self.scrollPlayedTrackIntoView = scrollPlayedTrackIntoView
        self.showingHidden = showingHidden
        self.loadTracksFunc = loadTracksFunc
        self.originalView = originalView
        self.artistFilter = artistFilter
        self.albumFilter = albumFilter
        self.user = user
        self.alwaysReload = alwaysReload
        self.lastOfflineMode = OfflineStorageService.offlineModeEnabled
        
        super.init(nibName: nil, bundle: nil)
        
        self.title = title
        
        // We hold track data fairly long term in this controller a lot of the time. Subscribe to broadcasts for track changes
        // so that we can update our track information in real time
        TrackService.observeTrackChanges(self) { vc, updatedTrack in
            if self.trackIdToTrack[updatedTrack.id] == nil {
                return
            }
            
            self.trackIdToTrack[updatedTrack.id] = updatedTrack
            
            DispatchQueue.main.async {
                self.tableView.visibleCells.forEach { cell in
                    let songViewCell = cell as! TrackViewCell
                    if songViewCell.track!.id == updatedTrack.id {
                        songViewCell.track = updatedTrack
                    }
                }
            }
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
