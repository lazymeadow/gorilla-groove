import UIKit

class MyLibraryController: UIViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad()

        self.title = "My Library"
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        GGNavLog.info("Loaded my library")
        
        loadInitialVc(.TITLE)
    }
    
    private func loadInitialVc(_ viewType: LibraryViewType) {
        MyLibraryController.loadTitleView(vc: self)

//        switch (viewType) {
//        case .TITLE:
//            loadTitleView("My Library")
//        case .ARTIST:
//            loadArtistView()
//        case .ALBUM:
//            loadAlbumView()
//        case .PLAY_COUNT:
//            loadTitleView("My Library", "play_count", sortAscending: false)
//        case .DATE_ADDED:
//            loadTitleView("My Library", "added_to_library", sortAscending: false)
//        }
    }
    
    static func loadTitleView(vc: UIViewController) {
        let tracks = TrackService.getTracks()

        let view = TrackViewController("My Library", tracks, originalView: .TITLE)
        view.tabBarItem = RootNavigationController.libraryTabBarItem
        vc.navigationController!.setViewControllers([view], animated: false)
    }
    
    static func loadArtistView(vc: UIViewController) {
        let artists = TrackDao.getArtists(isSongCached: OfflineStorageService.offlineModeEnabled ? true : nil)

        let view = ArtistViewController("My Library", artists)
        view.tabBarItem = RootNavigationController.libraryTabBarItem
        
        let backItem = UIBarButtonItem()
        backItem.title = "Artists"
        view.navigationItem.backBarButtonItem = backItem
        vc.navigationController!.setViewControllers([view], animated: false)
    }
    
    static func loadAlbumView(vc: UIViewController) {
        let albums = TrackDao.getAlbums(isSongCached: OfflineStorageService.offlineModeEnabled ? true : nil)

        let view = AlbumViewController("My Library", albums, nil)
        view.tabBarItem = RootNavigationController.libraryTabBarItem
        let backItem = UIBarButtonItem()
        backItem.title = "Albums"
        view.navigationItem.backBarButtonItem = backItem
        vc.navigationController!.setViewControllers([view], animated: false)
    }
    
    static func getNavigationOptions(vc: UIViewController, viewType: LibraryViewType) -> [FilterOption] {
        return [
            FilterOption("View by Name", isSelected: viewType == .TITLE) { MyLibraryController.loadTitleView(vc: vc) },
            FilterOption("View by Artist", isSelected: viewType == .ARTIST) { MyLibraryController.loadArtistView(vc: vc) },
            FilterOption("View by Album", isSelected: viewType == .ALBUM) { MyLibraryController.loadAlbumView(vc: vc) },
        ]
    }
}

enum LibraryViewType: CaseIterable {
    case TITLE
    case ARTIST
    case ALBUM
    case PLAY_COUNT
    case DATE_ADDED
    case PLAYLIST
    case NOW_PLAYING
}

