import Foundation
import UIKit
import Toast

class LogoutController : UIViewController {
    
    @SettingsBundleStorage(key: "offline_mode_enabled")
    private var offlineModeEnabled: Bool
    
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
        
        if offlineModeEnabled {
            GGNavLog.info("User was in offline mode and could not log out")
            
            AppDelegate.rootView?.makeToast("You are in offline mode and cannot be logged out")
            navigationController?.popViewController(animated: false)
        } else {
            logout()
        }
    }
    
    private func logout() {
        // Call stop() before we actually clear out login state as this updates the server with our device
        AudioPlayer.stop()
        UserState.isLoggedIn = false

        // TODO actually send the logout command to the API
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
