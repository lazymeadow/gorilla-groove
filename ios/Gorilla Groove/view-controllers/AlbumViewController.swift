import UIKit
import Foundation

// There is sort of a bug on this view that toggling on or off offline mode doesn't refresh the albums that we show.
// The artist and track views do. However, it isn't objectively better to fix this as it requires reworking the way
// that this controller works (as currently it can take Albums from an external source). And taking Albums from that
// external source can be good because it means that, when viewing another users' library, you can look at their
// albums for an artist without making a request. These files are just kind of a disaster and need to be reworked,
// but I'm so close with being done working on this that I'm just.... not going to. At least not right now.
class AlbumViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    private var albums: [Album] = []
    private var tracks: [Track]
    private var visibleAlbums: [Album] = []
    
    private var showHiddenTracks: Bool
    private var lastOfflineMode: Bool
    private var artist: String? = nil
    
    private lazy var filterOptions: [[FilterOption]] = [
        MyLibraryHelper.getNavigationOptions(vc: self, viewType: artist != nil ? .ARTIST : .ALBUM, user: user),
        [
            FilterOption("Show Hidden Tracks", filterImage: showHiddenTracks ? .CHECKED : .NONE) { [weak self] option in
                guard let this = self else { return }
                this.showHiddenTracks = !this.showHiddenTracks
                option.filterImage = this.showHiddenTracks ? .CHECKED : .NONE
                this.filter.reloadData()
                this.refreshAlbums()
            },
        ]
    ]
    
    private lazy var filter = TableFilter(filterOptions, vc: self)
    private let user: User?
    
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
            
            filter.topAnchor.constraint(equalTo: view.topAnchor),
            filter.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -10),
        ])
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(AlbumViewCell.self, forCellReuseIdentifier: "albumCell")
        
        TableSearchAugmenter.addSearchToNavigation(
            controller: self,
            tableView: tableView,
            onTap: { [weak self] in self?.filter.setIsHiddenAnimated(true) }
        ) { [weak self] input in
            self?.setVisibleAlbums()
        }
        
        tableView.tableFooterView = UIView(frame: .zero)
        
        // Because the footer has no size, set an additional handler on the controller's view to make sure tapping on empty space closes it
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(closeFilter))
        view.addGestureRecognizer(tapGesture)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        GGNavLog.info("Loaded album view")
        
        if albums.isEmpty || lastOfflineMode != OfflineStorageService.offlineModeEnabled {
            loadTracks()
        }
    }
    
    private func loadTracks() {
        activitySpinner.startAnimating()

        if let user = user {
            loadWebTracks(user: user)
        } else {
            DispatchQueue.global().async { [weak self] in
                guard let this = self else { return }
                
                // Always get hidden tracks (aka nil which gets both). We'll filter them out later if we don't want to see them.
                // This prevents us from having to reload tracks if we want to toggle them on or off
                this.tracks = TrackService.getTracks(showHidden: nil)
                DispatchQueue.main.async {
                    this.refreshAlbums()
                }
            }
        }
    }
    
    private func refreshAlbums() {
        albums = TrackService.getAlbumsFromTracks(tracks, showHidden: showHiddenTracks)
        setVisibleAlbums()
        
        DispatchQueue.main.async { [weak self] in
            self?.activitySpinner.stopAnimating()
            self?.tableView.reloadData()
        }
    }
    
    private func setVisibleAlbums() {
        let searchTerm = searchText.lowercased()
        if (searchTerm.isEmpty) {
            visibleAlbums = albums
        } else {
            visibleAlbums = albums.filter { $0.name.lowercased().contains(searchTerm) }
        }
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleAlbums.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "albumCell", for: indexPath) as! AlbumViewCell
        let album = visibleAlbums[indexPath.row]
        
        cell.tableIndex = indexPath.row
        cell.album = album
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        filter.setIsHiddenAnimated(true)
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! AlbumViewCell
        
        cell.animateSelectionColor()
        filter.setIsHiddenAnimated(true)

        // If someone picked the special "View All" album at the top, then load everything by nilling this out
        let albumToLoad = cell.album!.viewAllAlbum ? nil : cell.album!.name
        let viewName = cell.album!.viewAllAlbum ? artist! : albumToLoad!
                
        let view = TrackViewController(
            viewName,
            originalView: artist == nil ? .ALBUM : .ARTIST,
            artistFilter: artist,
            albumFilter: albumToLoad,
            user: user,
            showingHidden: showHiddenTracks
        )
        navigationController!.pushViewController(view, animated: true)
    }
    
    @objc private func closeFilter(sender: UITapGestureRecognizer) {
        filter.setIsHiddenAnimated(true)
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let albumViewCell = cell as! AlbumViewCell
        
        let album = visibleAlbums[indexPath.row]
        
        if (album.imageLoadFired || album.trackIdForArt == 0) {
            return
        }
        
        if album.artCached {
            if let artData = CacheService.getCachedData(trackId: album.trackIdForArt, cacheType: .thumbnail) {
                let art = UIImage(data: artData)
                album.art = art
                albumViewCell.album = album
                return
            }
            DispatchQueue.global().async {
                GGLog.error("Had cached art but failed to load it! Wiping out bad cache and fetching live art")
                TrackDao.setCachedAt(trackId: album.trackIdForArt, cachedAt: nil, cacheType: .thumbnail)
            }
        }
        
        album.imageLoadFired = true
        
        DispatchQueue.global().async {
            let linkRequestLink = "file/link/\(album.trackIdForArt)?artSize=SMALL&linkFetchType=ART"
            HttpRequester.get(linkRequestLink, TrackLinkResponse.self) { response, status, err in
                if (status < 200 || status >= 300) {
                    return
                }
                
                guard let artLink = response?.albumArtLink, let art = UIImage.fromUrl(artLink) else {
                    GGLog.error("Failed to load art from URL despite a status code of \(status)!")
                    return
                }
                guard let data = art.pngData() else {
                    GGLog.error("Failed to get PNG data from art!")
                    return
                }
                
                CacheService.setCachedData(trackId: album.trackIdForArt, data: data, cacheType: .thumbnail)
                DispatchQueue.main.async { [weak self] in
                    guard let this = self else { return }
                    
                    album.art = art
                    
                    let foundCell = this.tableView.visibleCells.first { rawCell in
                        let cell = rawCell as! AlbumViewCell
                        return cell.album?.unfilteredIndex == album.unfilteredIndex
                    }
                    
                    // The album isn't changing, but this forces a reload of the table cell to display the art
                    if foundCell != nil {
                        albumViewCell.album = album
                    }
                }
            }
        }
    }
    
    private func setAlbums(_ albums: [Album]) {
        var displayedAlbums = albums
        if (artist != nil && displayedAlbums.count > 1) {
            let viewAll = Album(
                name: "View All",
                trackIdForArt: 0,
                art: nil,
                imageLoadFired: false,
                viewAllAlbum: true,
                artCached: false
            )
            
            displayedAlbums.insert(viewAll, at: 0)
        }
        
        // We usually deal with "visibleAlbums", but because of the async nature of album network requests,
        // we look at the non-visible albums for album art as the source of truth. It would be inconvenient
        // to constantly be doing linear searches to find the right album, so store the original index.
        for (index, album) in displayedAlbums.enumerated() { album.unfilteredIndex = index }
        
        self.albums = displayedAlbums
        self.visibleAlbums = displayedAlbums
        
        tableView.reloadData()
        
        self.activitySpinner.stopAnimating()
    }
    
    private func loadWebTracks(user: User) {
        self.activitySpinner.startAnimating()
        
        let url = "track?userId=\(user.id)&sort=name,ASC&size=100000&page=0&showHidden=true"
        HttpRequester.get(url, LiveTrackRequest.self) { [weak self] res, status, _ in
            guard let this = self else { return }
            
            guard let trackResponse = res?.content, status.isSuccessful() else {
                this.albums = []
                this.visibleAlbums = []
                
                DispatchQueue.main.async {
                    Toast.show("Could not load albums")
                    this.tableView.reloadData()
                    this.activitySpinner.stopAnimating()
                }
                return
            }
            
            this.tracks = trackResponse.map { $0.asTrack(userId: user.id) }
            DispatchQueue.main.async {
                this.refreshAlbums()
            }
        }
    }
    
    init(
        _ title: String,
        _ albums: [Album],
        _ tracks: [Track] = [],
        _ artist: String?,
        user: User? = nil,
        showingHidden: Bool? = nil
    ) {
        self.artist = artist
        self.user = user
        self.showHiddenTracks = showingHidden ?? (user != nil)
        self.tracks = tracks
        self.lastOfflineMode = OfflineStorageService.offlineModeEnabled

        super.init(nibName: nil, bundle: nil)
        
        self.title = title
        
        if !albums.isEmpty {
            setAlbums(albums)
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

// Needs to be a class and not a struct, because we want a reference type here for easier handling of the
// async album fetching and filtering we do with the search bar
class Album {
    let name: String
    let trackIdForArt: Int
    var art: UIImage?
    var imageLoadFired: Bool
    var viewAllAlbum: Bool
    var unfilteredIndex: Int
    var artCached: Bool
    
    init(
        name: String,
        trackIdForArt: Int,
        art: UIImage? = nil,
        imageLoadFired: Bool = false,
        viewAllAlbum: Bool = false,
        unfilteredIndex: Int = -1,
        artCached: Bool
    ) {
        self.name = name
        self.trackIdForArt = trackIdForArt
        self.art = art
        self.imageLoadFired = imageLoadFired
        self.viewAllAlbum = viewAllAlbum
        self.unfilteredIndex = unfilteredIndex
        self.artCached = artCached
    }
}
