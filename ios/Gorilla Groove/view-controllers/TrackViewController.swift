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
    
    private let originalView: LibraryViewType
    
    private let sortsIndex = 1
    
    private lazy var filterOptions: [[FilterOption]] = {
        if originalView == .PLAYLIST {
            return []
        } else if originalView == .NOW_PLAYING {
            return []
        } else {
            sortOverrideKey = "name"
            return [
                MyLibraryHelper.getNavigationOptions(vc: self, viewType: originalView),
                [
                    FilterOption("Sort by Name", filterImage: .ARROW_UP) { option in
                        self.handleSortChange(option: option, key: "name", initialSortAsc: true)
                    },
                    FilterOption("Sort by Play Count") { option in
                        self.handleSortChange(option: option, key: "play_count", initialSortAsc: false)
                    },
                    FilterOption("Sort by Date Added") { option in
                        self.handleSortChange(option: option, key: "added_to_library", initialSortAsc: false)
                    },
                ],
                [
                    FilterOption("Show Cached Status") { _ in},
                    FilterOption("Show Offline Status") { _ in },
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
        self.filterOptions[self.sortsIndex].forEach { $0.filterImage = .NONE }
        
        if self.sortOverrideKey == key {
            self.sortDirectionAscending = !self.sortDirectionAscending
        } else {
            self.sortDirectionAscending = initialSortAsc
            self.sortOverrideKey = key
        }
        option.filterImage = self.sortDirectionAscending ? .ARROW_UP : .ARROW_DOWN
        
        self.filter!.reloadData()
        self.loadTracks()
    }
        
    override func viewDidLoad() {
        super.viewDidLoad()
        GGNavLog.info("Loaded track view")

        view.addSubview(tableView)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
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
            onTap: { self.filter?.setIsHiddenAnimated(true) }
        ) { input in
            let searchTerm = input.lowercased()
            if (searchTerm.isEmpty) {
                self.visibleTrackIds = self.trackIds
            } else {
                self.visibleTrackIds = self.trackIds.filter {
                    let track = self.trackIdToTrack[$0]!
                    return track.name.lowercased().contains(searchTerm)
                }
            }
        }
        
        // TODO should update this to use ObservationTokens. I think this probably doesn't get cleaned up properly with the current app
        NowPlayingTracks.addTrackChangeObserver { _ in
            DispatchQueue.main.async {
                self.tableView.visibleCells.forEach { cell in
                    let songViewCell = cell as! TrackViewCell
                    songViewCell.checkIfPlaying()
                }
            }
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        loadTracks()
        
        if scrollPlayedTrackIntoView && NowPlayingTracks.nowPlayingIndex >= 0 {
            let indexPath = IndexPath(row: NowPlayingTracks.nowPlayingIndex, section: 0)
            tableView.scrollToRow(at: indexPath, at: .middle, animated: false)
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
        
        if let search = self.navigationItem.searchController {
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
        
        let alert = TrackContextMenu.createMenuForTrack(track, parentVc: self) { newTrack in
            if newTrack == nil || (!self.showingHidden && newTrack!.isHidden) {
                GGLog.info("Hiding existing track from menu list in response to edit")
                self.visibleTrackIds.remove(at: tableIndex.row)
                DispatchQueue.main.async {
                    self.tableView.deleteRows(at: [tableIndex], with: .automatic)
                }
            }
        }
        
        ViewUtil.showAlert(alert)
    }
    
    func loadTracks() {
        DispatchQueue.global().async { [self] in
            let loadFunc = loadTracksFunc ?? { [self] in
                TrackService.getTracks(album: albumFilter, artist: artistFilter, sortOverrideKey: sortOverrideKey, sortAscending: sortDirectionAscending)
            }
            
            let tracks = loadFunc()
            self.trackIds = tracks.map { $0.id }
            self.trackIdToTrack = tracks.keyBy { $0.id }
            self.visibleTrackIds = self.trackIds
            
            DispatchQueue.main.async {
                self.tableView.reloadData()
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
        loadTracksFunc: (() -> [Track])? = nil
    ) {
        self.scrollPlayedTrackIntoView = scrollPlayedTrackIntoView
        self.showingHidden = showingHidden
        self.loadTracksFunc = loadTracksFunc
        self.originalView = originalView
        self.artistFilter = artistFilter
        self.albumFilter = albumFilter
        
        super.init(nibName: nil, bundle: nil)
        
        loadTracks()

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
