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
    private let userId: Int
    
    private let originalView: LibraryViewType
    
    private let sortsIndex = 1
    
    // This never gets invoked. I've got a retain cycle somewhere but the memory graph in xcode crashes the shitty program
    // so debugging where this is has been proving a challenge.
    deinit {
        GGLog.info("Deinit TVC")
    }
    
    private lazy var filterOptions: [[FilterOption]] = {
        if originalView == .PLAYLIST {
            return []
        } else if originalView == .NOW_PLAYING {
            return []
        } else if originalView == .USER {
            sortOverrideKey = "name"
            return [
                [
                    FilterOption("Sort by Name", filterImage: .ARROW_UP) { [weak self] option in
                        self?.handleSortChange(option: option, key: "name", initialSortAsc: true)
                    },
                    FilterOption("Sort by Play Count") { [weak self] option in
                        self?.handleSortChange(option: option, key: "playCount", initialSortAsc: false)
                    },
                    FilterOption("Sort by Date Added") { [weak self] option in
                        self?.handleSortChange(option: option, key: "addedToLibrary", initialSortAsc: false)
                    },
                ]
            ]
        } else {
            sortOverrideKey = "name"
            return [
                MyLibraryHelper.getNavigationOptions(vc: self, viewType: originalView),
                [
                    FilterOption("Sort by Name", filterImage: .ARROW_UP) { [weak self] option in
                        self?.handleSortChange(option: option, key: "name", initialSortAsc: true)
                    },
                    FilterOption("Sort by Play Count") { [weak self] option in
                        self?.handleSortChange(option: option, key: "play_count", initialSortAsc: false)
                    },
                    FilterOption("Sort by Date Added") { [weak self] option in
                        self?.handleSortChange(option: option, key: "added_to_library", initialSortAsc: false)
                    },
                ]
            ]
        }
    }()
    
    private lazy var filter: TableFilter? = {
        if !filterOptions.isEmpty {
            return TableFilter(filterOptions, vc: self)
        } else {
            return nil
        }
    }()
    
    private func handleSortChange(option: FilterOption, key: String, initialSortAsc: Bool) {
        // This is so dumb don't hate me
        filterOptions[userId == 0 ? 1 : 0].forEach { $0.filterImage = .NONE }
        
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
        
        // TODO should update this to use ObservationTokens. I think this probably doesn't get cleaned up properly with the current app
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
        
        loadTracks()
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
    
    func loadTracks() {
        activitySpinner.startAnimating()
        
        if userId == 0 {
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
                }
            }
        }
    }
    
    private func loadWebTracks() {
        let sortDir = sortDirectionAscending ? "ASC" : "DESC"
        let url = "track?userId=\(userId)&sort=\(sortOverrideKey!),\(sortDir)&size=100000&page=0&showHidden=true"
        HttpRequester.get(url, LiveTrackRequest.self) { [weak self] res, status, _ in
            guard let this = self else { return }
            guard let tracks = res?.content, status.isSuccessful() else {
                DispatchQueue.main.async {
                    Toast.show("Could not load tracks")
                }
                this.trackIds = []
                this.trackIdToTrack = [:]
                this.visibleTrackIds = []
                this.tableView.reloadData()
                return
            }
            
            this.trackIds = tracks.map { $0.id }
            this.trackIdToTrack = tracks.map { $0.asTrack(userId: this.userId) }.keyBy { $0.id }
            this.visibleTrackIds = this.trackIds
            
            DispatchQueue.main.async {
                this.activitySpinner.stopAnimating()
                this.tableView.reloadData()
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
        userId: Int = 0,
        loadTracksFunc: (() -> [Track])? = nil
    ) {
        self.scrollPlayedTrackIntoView = scrollPlayedTrackIntoView
        self.showingHidden = showingHidden
        self.loadTracksFunc = loadTracksFunc
        self.originalView = originalView
        self.artistFilter = artistFilter
        self.albumFilter = albumFilter
        self.userId = userId
        
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
