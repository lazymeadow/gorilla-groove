import Foundation
import UIKit
import InAppSettingsKit

class RootNavigationController : UITabBarController {
        
    let libraryController = MyLibraryController()
    let nowPlayingController = NowPlayingController()
    let usersController = UsersController()
    let playlistsController = PlaylistsController()
    let appSettingsViewController = IASKAppSettingsViewController()
        
    func getNowPlayingSongController() -> TrackViewController {
        return TrackViewController("Now Playing", NowPlayingTracks.nowPlayingTracks, scrollPlayedTrackIntoView: true)
    }
    
    override func viewDidLoad() {
        GGNavLog.info("Loaded root navigation")
        super.viewDidLoad()
        
        // The library adds a "Done" button as the right nav item. We don't need this
        appSettingsViewController.showDoneButton = false
        
        let mediaControls = MediaControlsController()
        let middleBar = createMiddleBar()
        
        self.tabBar.isTranslucent = false
        // Use this instead of "backgrounColor". For whatever reason "backgroundColor" does not use the color you specify. It's slightly wrong?
        self.tabBar.barTintColor = UIColor(named: "Nav Controls")
        self.tabBar.tintColor = Colors.secondary // Icon and text color when selected
        self.tabBar.unselectedItemTintColor = Colors.whiteTransparent

        libraryController.tabBarItem = UITabBarItem(title: "My Library", image: UIImage(systemName: "music.house.fill"), tag: 0)
        nowPlayingController.tabBarItem = UITabBarItem(title: "Now Playing", image: UIImage(systemName: "music.note"), tag: 1)
        usersController.tabBarItem = UITabBarItem(title: "Users", image: UIImage(systemName: "person.3.fill"), tag: 2)
        playlistsController.tabBarItem = UITabBarItem(title: "Playlists", image: UIImage(systemName: "music.note.list"), tag: 3)
        appSettingsViewController.tabBarItem = UITabBarItem(title: "Settings", image: UIImage(systemName: "gear"), tag: 4)

        self.viewControllers = [
            libraryController,
            nowPlayingController,
            usersController,
            playlistsController,
            appSettingsViewController
        ].map { vc in
            UINavigationController(rootViewController: vc)
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
        view.backgroundColor = UIColor(named: "Nav Controls Divider")
        
        view.heightAnchor.constraint(equalToConstant: 1).isActive = true
        
        return view
    }
}
