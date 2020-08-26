import UIKit
import CoreData

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        UINavigationBar.appearance().barTintColor = UIColor(named: "Navigation Background")
        UINavigationBar.appearance().tintColor = Colors.primary
        UINavigationBar.appearance().isTranslucent = false
        
        AudioPlayer.initialize()
        
        // Make sure we have a device ID generated for the app
        if (FileState.read(DeviceState.self) == nil) {
            FileState.save(DeviceState(deviceId: UUID().uuidString.lowercased()))
        }

        if let loginState = FileState.read(LoginState.self) {
            print("User was previously logged in")
            Database.openDatabase(userId: loginState.id)
            
            let user = UserState.getOwnUser()
            if (user.lastSync == nil) {
                if AppDelegate.getAppVersion() == "1.3.0.1" {
                    print("User is on 1.3.0.1 and needs to resync to fix a bug. Going to sync screen")
                    window!.rootViewController = SyncController()
                } else {
                    print("Somehow logged in without ever syncing. Seems like a bad thing. Going to login screen again instead")
                }
            } else {
                window!.rootViewController = RootNavigationController()
            }
        }

        return true
    }
    
    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        print("Application will terminate")
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
        // Saves changes in the application's managed object context before the application terminates.
    }
    
    static func getAppVersion() -> String {
        let appVersionString = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String
        let buildNumber = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as! String
        return "\(appVersionString).\(buildNumber)"
    }
}

