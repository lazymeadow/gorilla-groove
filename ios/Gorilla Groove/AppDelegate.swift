import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        GGLogger.initialize()
        CrashReportService.initialize()
        
        GGLog.info("\n \nApp was booted\n \n")
        
        UINavigationBar.appearance().barTintColor = Colors.navigationBackground
        UINavigationBar.appearance().tintColor = Colors.primary
        UINavigationBar.appearance().isTranslucent = false
        
        AudioPlayer.initialize()
        NowPlayingTracks.initialize()
        LocationService.initialize()
        OfflineStorageService.initialize()
        
        // Make sure we have a device ID generated for the app
        if (FileState.read(DeviceState.self) == nil) {
            FileState.save(DeviceState(deviceId: UUID().uuidString.lowercased()))
        }

        if let loginState = FileState.read(LoginState.self) {
            GGLog.debug("User was previously logged in")
            Database.openDatabase(userId: loginState.id)

            if (ServerSynchronizer.lastSync == Date.minimum()) {
                if AppDelegate.getAppVersion() == "1.3.1.1" {
                    GGLog.info("User is on 1.3.0.1 and needs to resync to fix a bug. Going to sync screen")
                    window!.rootViewController = SyncController()
                    UserState.isLoggedIn = true
                } else {
                    GGLog.warning("Somehow logged in without ever syncing. Seems like a bad thing. Going to login screen again instead")
                }
            } else {
                UserState.isLoggedIn = true
                window!.rootViewController = RootNavigationController()
            }
        }
        
        SettingsService.initialize()

        return true
    }
    
    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
        
        // Seems to fire at the same time as applicationDidEnterBackground, so can't use it for stuff like Socket destruction
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        GGLog.debug("App is now in the background")
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        GGLog.debug("App is now in the foreground")
        if UserState.isLoggedIn {
            DispatchQueue.global().async {
                let syncsPerformed = ServerSynchronizer.syncWithServer(abortIfRecentlySynced: true)
                
                // Only want to do this if the sync didn't run, as the sync will kick this off afterwards
                if !syncsPerformed.contains(.track) && OfflineStorageService.backgroundDownloadInterrupted {
                    OfflineStorageService.downloadAlwaysOfflineMusic()
                }
            }
        }
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        GGLog.info("Application is terminating")
    }
    
    typealias SigactionHandler = @convention(c)(Int32) -> Void
    
    func applicationDidFinishLaunching(_ application: UIApplication) {
//        print("Did finish launching")
    }

    static func getAppVersion() -> String {
        let appVersionString = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String
        let buildNumber = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as! String
        return "\(appVersionString).\(buildNumber)"
    }
    
    static var rootView: UIView? {
        get {
            (UIApplication.shared.delegate as? AppDelegate)?.window?.rootViewController?.view
        }
    }
}

