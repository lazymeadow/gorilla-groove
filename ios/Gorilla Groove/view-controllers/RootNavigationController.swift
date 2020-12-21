import Foundation
import UIKit
import InAppSettingsKit
import AVFoundation

class RootNavigationController : UITabBarController {
    
    @SettingsBundleStorage(key: "launch_screen")
    private var launchScreenTag: Int
  
    private let LOGOUT_TAG = 7
    
    private lazy var tagToController: [Int: UIViewController] = {
        let libraryController = MyLibraryController()
        let nowPlayingController = NowPlayingController()
        let usersController = UsersController()
        let playlistsController = PlaylistsController()
        let appSettingsController = IASKAppSettingsViewController()
        let reviewQueueController = ReviewQueueController()
        let errorReportController = ErrorReportController()
        let logoutController = LogoutController()

        // Do NOT edit these tag values. They are used for keeping track of the order of the VCs in the tab bar, and changing the tags
        // would change how a user sees them. The tag is also used for the "launch_screen" setting value
        
        libraryController.tabBarItem = UITabBarItem(title: "My Library", image: UIImage(systemName: "music.house.fill"), tag: 0)
        nowPlayingController.tabBarItem = UITabBarItem(title: "Now Playing", image: UIImage(systemName: "music.note"), tag: 1)
        usersController.tabBarItem = UITabBarItem(title: "Users", image: UIImage(systemName: "person.3.fill"), tag: 2)
        playlistsController.tabBarItem = UITabBarItem(title: "Playlists", image: UIImage(systemName: "music.note.list"), tag: 3)
        appSettingsController.tabBarItem = UITabBarItem(title: "Settings", image: UIImage(systemName: "gear"), tag: 4)
        
        reviewQueueController.tabBarItem = UITabBarItem(title: "Review Queue", image: UIImage(systemName: "headphones"), tag: 5)
        errorReportController.tabBarItem = UITabBarItem(title: "Problem Report", image: UIImage(systemName: "exclamationmark.triangle.fill"), tag: 6)
        logoutController.tabBarItem = UITabBarItem(title: "Logout", image: UIImage(systemName: "arrow.down.right.square"), tag: LOGOUT_TAG)
        
        // The library adds a "Done" button as the right nav item. We don't need this
        appSettingsController.showDoneButton = false
        
        return [
            libraryController.tabBarItem.tag: libraryController,
            nowPlayingController.tabBarItem.tag: nowPlayingController,
            usersController.tabBarItem.tag: usersController,
            playlistsController.tabBarItem.tag: playlistsController,
            appSettingsController.tabBarItem.tag: appSettingsController,
            reviewQueueController.tabBarItem.tag: reviewQueueController,
            errorReportController.tabBarItem.tag: errorReportController,
            logoutController.tabBarItem.tag: logoutController
        ]
    }()
    
    // Tried to just use tagToController.values but Swift has no LinkedHashMap equivalent unfortunately so the order is random
    private lazy var defaultOrderedControllers: [UIViewController] = {
        return [
            tagToController[0]!,
            tagToController[1]!,
            tagToController[2]!,
            tagToController[3]!,
            tagToController[4]!,
            tagToController[5]!,
            tagToController[6]!,
            tagToController[LOGOUT_TAG]!
        ]
    }()
    
    private var movedLaunchScreen = false
    
    // This is the height of this block of unmoving content on the bottom of the screen- the media controls,
    // the tab bar, and the middle dividing bar. It seems like a pain in the ass to dynamically get the heights,
    // so I am hard coding this and will probably regret it later.
    private let bottomContentHeight = CGFloat(141.0)
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        GGNavLog.info("Loaded root navigation")
        
        let mediaControls = MediaControlsController()
        let middleBar = createMiddleBar()
        
        self.tabBar.isTranslucent = false
        // Use this instead of "backgrounColor". For whatever reason "backgroundColor" does not use the color you specify. It's slightly wrong?
        self.tabBar.barTintColor = Colors.navControls
        self.tabBar.tintColor = Colors.secondary // Icon and text color when selected
        self.tabBar.unselectedItemTintColor = Colors.whiteTransparent
                
        self.viewControllers = getRestoredOrder().map { vc in
            // Most of the views have some form of navigation, so wrap them in a navigation controller.
            // Copied this off the interweb, but it might make more sense to just individually wrap the
            // controllers that need it. Probably do that later.
            let controller = UINavigationController(rootViewController: vc)
            
            // In order to not have this content hide behind the media controls, add an offset to the bottom of the views.
            // Pretty hacky, but I stole the hackiness from https://stackoverflow.com/questions/42384470/view-on-top-of-uitabbar
            controller.additionalSafeAreaInsets = UIEdgeInsets(top: 0, left: 0, bottom: bottomContentHeight, right: 0)
            
            return controller
        }
        self.customizableViewControllers = self.viewControllers!.filter { $0.tabBarItem.tag != LOGOUT_TAG }
        
        self.delegate = self
        
        // Users can specify in settings which screen is their launch screen (defaults to My Library)
        if let launchScreenIndex = self.viewControllers?.firstIndex(where: { vc in vc.tabBarItem.tag == launchScreenTag }) {
            self.selectedIndex = launchScreenIndex
        } else {
            GGLog.error("Could not find launch screen for tag \(launchScreenTag)!")
        }
        
        // Style the controller so it fits with the rest of the app
        if let moreMenu = self.moreNavigationController.topViewController?.view as? UITableView {
            moreMenu.tableFooterView = UIView(frame: .zero)
            moreMenu.tintColor = Colors.tableText
        } else {
            GGLog.critical("Could not load More menu to customize!")
        }
        
        self.moreNavigationController.additionalSafeAreaInsets = UIEdgeInsets(top: 0, left: 0, bottom: bottomContentHeight, right: 0)
        
        
        // This comment changes the color of the "Edit" button in the navbar. Not sure what color to make it right now.
        // self.moreNavigationController.navigationBar.tintColor = Colors.secondary
        

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
        
        if movedLaunchScreen {
            guard let activeVcName = self.viewControllers?.first?.tabBarItem.title else {
                GGLog.critical("Could not load active view controller when alerting user we changed their setup!")
                return
            }
            
            ViewUtil.showAlert(
                title: "Tab Bar Changed",
                message: "The '\(activeVcName)' tab is configured to be your default launch screen, but it was inside of the 'More' menu. It has been moved to be your first tab."
            )

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
        GGLog.info("User changed order of Tab Bar items. Saving new order")
        UserDefaults.standard.set(order, forKey: "TabBarItemsOrder")
    }
    
    private func getRestoredOrder() -> [UIViewController] {
        guard let order = UserDefaults.standard.value(forKey: "TabBarItemsOrder") as? [Int] else {
            // First time running the app (unless we had a bad error). Return the default configuration
            return defaultOrderedControllers
        }
        
        var reorderedControllers: [UIViewController] = order.compactMap { tag in
            if let controller = tagToController[tag] {
                return controller
            } else {
                GGLog.warning("Could not find view controller with tag \(tag) while restoring tab bar layout. Was it removed?")
                return nil
            }
        }
        
        if reorderedControllers.count != tagToController.values.count {
            GGLog.warning("Reordered controller count did not match total controller count. Assuming controllers need to be added")
            let currentTags = Set(order)
            
            tagToController.values.forEach { controller in
                if !currentTags.contains(controller.tabBarItem.tag) {
                    GGLog.info("Adding '\(controller.tabBarItem.title ?? "nil")' controller...")
                    reorderedControllers.append(controller)
                }
            }
            
            // Keep the "sign out" controller at the bottom, always
            reorderedControllers.removeAll() { $0.tabBarItem.tag == LOGOUT_TAG }
            reorderedControllers.append(tagToController[LOGOUT_TAG]!)
            
            saveOrder(order: reorderedControllers.map { $0.tabBarItem.tag })
        }
        
        if let indexOfLaunchScreen = reorderedControllers.firstIndex(where: { vc in vc.tabBarItem.tag == launchScreenTag}) {
            // If the controller is in the "more" menu, that is no bueno as it causes issues initializing the "more" menu's styling.
            // It's also just highly weird that a user would want a launch screen that is nested inside of the "more" menu, so we
            // are going to aggressively change their tabs on them and put their default screen as the first tab if it was in the "more" menu
            if indexOfLaunchScreen > 3 {
                reorderedControllers.swapAt(0, indexOfLaunchScreen)
                movedLaunchScreen = true
                
                saveOrder(order: reorderedControllers.map { $0.tabBarItem.tag })
            }
        }
        
        return reorderedControllers
    }
}
