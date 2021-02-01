import Foundation
import UIKit

// Not a fan of this being a view controller but it is what it is as long as we're using Apple's bottom bar
class LogoutController : UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Logout"
        
        // The background is already the "background" color, because it inherits. But without explicitly setting
        // the background, some of the "more" menu's table lines bleed over during the transition and it looks ugly
        self.view.backgroundColor = Colors.background
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        GGNavLog.info("Loaded logout controller")
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        if OfflineStorageService.offlineModeEnabled {
            GGNavLog.info("User was in offline mode and could not log out")
            
            Toast.show("You are in offline mode and cannot be logged out")
            navigationController?.popViewController(animated: false)
        } else {
            confirmLogoutViaAlert()
        }
    }

    private func confirmLogoutViaAlert() {
        let alert = UIAlertController(
            title: "Logout?",
            message: "Are you sure you'd like to logout?",
            preferredStyle: .alert
        )

        alert.addAction(.init(
            title: "Yee",
            style: .destructive,
            handler: { (action: UIAlertAction) in
                self.logout()
            }
        ))

        alert.addAction(.init(
            title: "Nah",
            style: .cancel,
            handler: { (action: UIAlertAction) in
                self.navigationController?.popViewController(animated: true)
            }
        ))

        self.present(alert, animated: true)
    }
    
    private func logout() {
        // Call stop() before we actually clear out login state as this updates the server with our device
        AudioPlayer.stop()
        UserState.isLoggedIn = false

        HttpRequester.post("authentication/logout", EmptyResponse.self, nil) { _, statusCode, _ in
            if (statusCode == 200) {
                GGLog.info("Logout token deleted")
            }
        }
        FileState.clear(LoginState.self)
        WebSocket.disconnect()
        
        // Until we ditch the storyboard, have to navigate to the login view this way
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "LoginController")
        
        // When I used the present() code, it would make the Sync view not load properly
        // after someone logged out? It seems like setting the root view is the only way
        // to navigate anywhere (outside a navigation controller) that doesn't have side effects...
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        appDelegate.window!.rootViewController = vc
//        vc.modalPresentationStyle = .fullScreen
//        vc.modalTransitionStyle = .crossDissolve
//        self.present(vc, animated: true)
        
        Database.close()
    }
}
