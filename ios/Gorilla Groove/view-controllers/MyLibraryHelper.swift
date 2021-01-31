import UIKit

class MyLibraryHelper {

    // I have issues with retain cycles, and I can't debug them because the memory graph in xcode crashes
    // whenever I try to use it. This is somewhat of a stop-gap measure to reduce the impact of the memory leak.
    // UPDATE this has gotten hackier since I wrote the first two lines of the comment. Idk what to do anymore.
    static var titleView = TrackViewController("My Library", originalView: .TITLE)
    
    static func loadTitleView(vc: UIViewController, user: User?) {
        if let user = user {
            let view = TrackViewController(user.name, originalView: .TITLE, user: user)
            vc.navigationController!.replaceLastWith(view)
        } else {
            vc.navigationController!.replaceLastWith(titleView)
        }
    }
    
    static func loadArtistView(vc: UIViewController, user: User?) {
        let title = user == nil ? "My Library" : user!.name

        let view = ArtistViewController(title, user: user)
        
        let backItem = UIBarButtonItem()
        backItem.title = "Artists"
        view.navigationItem.backBarButtonItem = backItem
        vc.navigationController!.replaceLastWith(view)
    }
    
    static func loadAlbumView(vc: UIViewController, user: User?) {
        var albums: [Album] = []
        if user == nil {
            albums = TrackService.getAlbumsFromTracks(TrackService.getTracks())
        }
        
        let title = user == nil ? "My Library" : user!.name
        
        let view = AlbumViewController(title, albums, nil, user: user)
        let backItem = UIBarButtonItem()
        backItem.title = "Albums"
        view.navigationItem.backBarButtonItem = backItem
        vc.navigationController!.replaceLastWith(view)
    }
    
    static func getNavigationOptions(vc: UIViewController, viewType: LibraryViewType, user: User?) -> [FilterOption] {
        weak var vc = vc
        return [
            FilterOption("View by Track", filterImage: viewType == .TITLE ? .CHECKED : .NONE) { _ in
                if let vc = vc {
                    loadTitleView(vc: vc, user: user)
                }
            },
            FilterOption("View by Artist", filterImage: viewType == .ARTIST ? .CHECKED : .NONE) { _ in
                if let vc = vc {
                    loadArtistView(vc: vc, user: user)
                }
            },
            FilterOption("View by Album", filterImage: viewType == .ALBUM ? .CHECKED : .NONE) { _ in
                if let vc = vc {
                    loadAlbumView(vc: vc, user: user)
                }
            },
        ]
    }
}

enum LibraryViewType: CaseIterable {
    case TITLE
    case ARTIST
    case ALBUM
    case PLAYLIST
    case NOW_PLAYING
}

extension UINavigationController {
    func replaceLastWith(_ controller: UIViewController) {
        if viewControllers.isEmpty {
            self.setViewControllers([controller], animated: false)
        } else {
            // We don't ever want the bottom bar to change as a result of these views switching. So keep the tab bar item going to the next
            let lastVc = self.viewControllers.last!
            if let tabBarItem = lastVc.tabBarItem {
                controller.tabBarItem = tabBarItem
            }
            
            var newControllers = Array(self.viewControllers.dropLast())
            newControllers.append(controller)
            self.setViewControllers(newControllers, animated: false)
        }
    }
}
