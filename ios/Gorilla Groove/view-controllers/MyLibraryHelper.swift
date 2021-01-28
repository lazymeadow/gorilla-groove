import UIKit

class MyLibraryHelper {

    static func loadTitleView(vc: UIViewController) {
        let view = TrackViewController("My Library", originalView: .TITLE)
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
            FilterOption("View by Name", filterImage: viewType == .TITLE ? .CHECKED : .NONE) { _ in
                loadTitleView(vc: vc)
            },
            FilterOption("View by Artist", filterImage: viewType == .ARTIST ? .CHECKED : .NONE) { _ in
                loadArtistView(vc: vc)
            },
            FilterOption("View by Album", filterImage: viewType == .ALBUM ? .CHECKED : .NONE) { _ in
                loadAlbumView(vc: vc)
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

