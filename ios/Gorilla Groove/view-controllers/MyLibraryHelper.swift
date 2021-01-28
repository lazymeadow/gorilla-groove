import UIKit

class MyLibraryHelper {

    // I have issues with retain cycles, and I can't debug them because the memory graph in xcode crashes
    // whenever I try to use it. This is somewhat of a stop-gap measure to reduce the impact of the memory leak.
    // The artist and album views aren't reused as I'd need to tweak them to not take dynamic constructor args
    static var titleView = TrackViewController("My Library", originalView: .TITLE)
    
    static func loadTitleView(vc: UIViewController) {
        titleView.tabBarItem = RootNavigationController.libraryTabBarItem
        vc.navigationController!.setViewControllers([titleView], animated: false)
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
        weak var vc = vc
        return [
            FilterOption("View by Name", filterImage: viewType == .TITLE ? .CHECKED : .NONE) { _ in
                if let vc = vc {
                    loadTitleView(vc: vc)
                }
            },
            FilterOption("View by Artist", filterImage: viewType == .ARTIST ? .CHECKED : .NONE) { _ in
                if let vc = vc {
                    loadArtistView(vc: vc)
                }
            },
            FilterOption("View by Album", filterImage: viewType == .ALBUM ? .CHECKED : .NONE) { _ in
                if let vc = vc {
                    loadAlbumView(vc: vc)
                }
            },
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

