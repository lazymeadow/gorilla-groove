import Foundation
import UIKit
import InAppSettingsKit
import AVFoundation

class RootNavigationController : UITabBarController {
    
    private let libraryController = MyLibraryController()
    private let nowPlayingController = NowPlayingController()
    private let usersController = UsersController()
    private let playlistsController = PlaylistsController()
    private let appSettingsViewController = IASKAppSettingsViewController()
    
    private let reviewQueueController = ReviewQueueController()
    private let errorReportController = ErrorReportController()
    
    // This is the height of this block of unmoving content on the bottom of the screen- the media controls,
    // the tab bar, and the middle dividing bar. It seems like a pain in the ass to dynamically get the heights,
    // so I am hard coding this and will probably regret it later.
    private let bottomContentHeight = CGFloat(141.0)
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        GGNavLog.info("Loaded root navigation")
        
        // The library adds a "Done" button as the right nav item. We don't need this
        appSettingsViewController.showDoneButton = false
        
        let mediaControls = MediaControlsController()
        let middleBar = createMiddleBar()
        
        self.tabBar.isTranslucent = false
        // Use this instead of "backgrounColor". For whatever reason "backgroundColor" does not use the color you specify. It's slightly wrong?
        self.tabBar.barTintColor = Colors.navControls
        self.tabBar.tintColor = Colors.secondary // Icon and text color when selected
        self.tabBar.unselectedItemTintColor = Colors.whiteTransparent
        
        libraryController.tabBarItem = UITabBarItem(title: "My Library", image: UIImage(systemName: "music.house.fill"), tag: 0)
        nowPlayingController.tabBarItem = UITabBarItem(title: "Now Playing", image: UIImage(systemName: "music.note"), tag: 1)
        usersController.tabBarItem = UITabBarItem(title: "Users", image: UIImage(systemName: "person.3.fill"), tag: 2)
        playlistsController.tabBarItem = UITabBarItem(title: "Playlists", image: UIImage(systemName: "music.note.list"), tag: 3)
        appSettingsViewController.tabBarItem = UITabBarItem(title: "Settings", image: UIImage(systemName: "gear"), tag: 4)
        
        reviewQueueController.tabBarItem = UITabBarItem(title: "Review Queue", image: UIImage(systemName: "headphones"), tag: 5)
        errorReportController.tabBarItem = UITabBarItem(title: "Problem Report", image: UIImage(systemName: "exclamationmark.triangle.fill"), tag: 6)
        
        self.viewControllers = [
            libraryController,
            nowPlayingController,
            usersController,
            playlistsController,
            appSettingsViewController,
            reviewQueueController,
            errorReportController
        ].map { vc in
            // Most of the views have some form of navigation, so wrap them in a navigation controller.
            // Copied this off the interweb, but it might make more sense to just individually wrap the
            // controllers that need it. Probably do that later.
            let controller = UINavigationController(rootViewController: vc)
            
            // In order to not have this content hide behind the media controls, add an offset to the bottom of the views.
            // Pretty hacky, but I stole the hackiness from https://stackoverflow.com/questions/42384470/view-on-top-of-uitabbar
            controller.additionalSafeAreaInsets = UIEdgeInsets(top: 0, left: 0, bottom: bottomContentHeight, right: 0)
            
            return controller
        }
        
        self.delegate = self
        restoreOrder()
        
        // Tell the tab bar to use the first VC as the "initial" one. Otherwise it'll use whatever VC was first in the above
        // array, which will likely always be the libraryController. But if we do this we give the user the freedom to set
        // their launch tab by setting it as the first one.
        self.selectedIndex = 0
        
        // This comment changes the color of the "Edit" button in the navbar. Not sure what color to make it right now.
        // self.moreNavigationController.navigationBar.tintColor = Colors.secondary
        
        // The "more" navigation controller requires its own nonsense apparently. Without this, the controllers viewed
        // from within the navigation controller are too large and have hidden content.
        self.moreNavigationController.additionalSafeAreaInsets = UIEdgeInsets(top: 0, left: 0, bottom: bottomContentHeight, right: 0)
        if let moreMenu = self.moreNavigationController.topViewController?.view as? UITableView {
            moreMenu.tableFooterView = UIView(frame: .zero)
            moreMenu.tintColor = Colors.tableText
        } else {
            GGLog.critical("Could not load More menu to customize!")
        }
        
        view.addSubview(mediaControls.view)
        view.addSubview(middleBar)
        
        mediaControls.view.translatesAutoresizingMaskIntoConstraints = false
        mediaControls.view.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        mediaControls.view.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        mediaControls.view.bottomAnchor.constraint(equalTo: middleBar.topAnchor).isActive = true
        
        middleBar.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        middleBar.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        middleBar.bottomAnchor.constraint(equalTo: tabBar.topAnchor).isActive = true
        
        try! AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try! AVAudioSession.sharedInstance().setActive(true)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        DispatchQueue.global().async {
            // Post the device before we kick off the sync. Sometimes the API needs to update data when we are on a new version
            // so this will make sure our first sync of the login will have the good stuff, instead of having to wait for the next sync.
            UserState.postCurrentDevice() {
                // No need to do this in a blocking way. It shouldn't be syncing all that much stuff.
                // If we block, there is a second delay or so before we can start using the app no matter what.
                ServerSynchronizer.syncWithServer()
            }
        }
    }
    
    private func createMiddleBar() -> UIView {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = Colors.navControlsDivider
        
        view.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        return view
    }
}

// This contains all the code for dealing with persisting and loading the order of the VCs on the tab bar
extension RootNavigationController: UITabBarControllerDelegate {
    override func tabBar(_ tabBar: UITabBar, didEndCustomizing items: [UITabBarItem], changed: Bool) {
        let order = getCurrentOrder()
        saveOrder(order: order)
    }
    
    private func getCurrentOrder() -> [Int] {
        return self.viewControllers?.map { $0.tabBarItem.tag } ?? []
    }
    
    private func saveOrder(order: [Int]) {
        UserDefaults.standard.set(order, forKey: "TabBarItemsOrder")
    }
    
    private func restoreOrder() {
        guard let order = UserDefaults.standard.value(forKey: "TabBarItemsOrder") as? [Int] else { return }
        
        guard let controllers = self.viewControllers else {
            GGLog.critical("Could not find controllers on UITabBarController to reorder!")
            return
        }
        
        let reorderedControllers: [UIViewController] = order.compactMap { tag in
            if let controller = controllers.first(where: { $0.tabBarItem.tag == tag }) {
                return controller
            } else {
                GGLog.warning("Could not find view controller with tag \(tag) while restoring tab bar layout. Was it removed?")
                return nil
            }
        }
        
        self.setViewControllers(reorderedControllers, animated: false)
    }
}
