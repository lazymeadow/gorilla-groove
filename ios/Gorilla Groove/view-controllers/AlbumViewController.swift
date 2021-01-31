import UIKit
import Foundation

class AlbumViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    private var albums: [Album] = []
    private var visibleAlbums: [Album] = []
    
    private var artist: String? = nil
    
    private lazy var filterOptions = [
        MyLibraryHelper.getNavigationOptions(vc: self, viewType: artist != nil ? .ARTIST : .ALBUM, user: user),
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
        tableView.register(AlbumViewCell.self, forCellReuseIdentifier: "albumCell")
        
        TableSearchAugmenter.addSearchToNavigation(
            controller: self,
            tableView: tableView,
            onTap: { [weak self] in self?.filter.setIsHiddenAnimated(true) }
        ) { [weak self] input in
            guard let this = self else { return }
            
            let searchTerm = input.lowercased()
            if (searchTerm.isEmpty) {
                this.visibleAlbums = this.albums
            } else {
                this.visibleAlbums = this.albums.filter { $0.name.lowercased().contains(searchTerm) }
            }
        }
        
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        GGNavLog.info("Loaded album view")
        
        if let user = user, albums.isEmpty {
            getUserAlbums(user)
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
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
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
        let viewName = cell.album!.viewAllAlbum ? "All " + artist! : albumToLoad!
                
        let view = TrackViewController(
            viewName,
            originalView: artist == nil ? .ALBUM : .ARTIST,
            artistFilter: artist,
            albumFilter: albumToLoad,
            user: user
        )
        navigationController!.pushViewController(view, animated: true)
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
            let linkRequestLink = "file/link/\(album.trackIdForArt)?artSize=SMALL"
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
    
    private func getUserAlbums(_ user: User) {
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
            
            let tracks = trackResponse.map { $0.asTrack(userId: user.id) }
            DispatchQueue.main.async {
                this.setAlbums(TrackService.getAlbumsFromTracks(tracks))
            }
        }
    }
    
    init(_ title: String, _ albums: [Album], _ artist: String?, user: User? = nil) {
        self.artist = artist
        self.user = user
        
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
