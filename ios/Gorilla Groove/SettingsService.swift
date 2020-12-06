import Foundation
import UIKit

// Pretty slick, and stolen from
// https://stackoverflow.com/a/64712479/13175115
@propertyWrapper
public struct SettingsBundleStorage<T> {
    private let key: String

    public init(key: String) {
        self.key = key
        setBundleDefaults()
    }

    public var wrappedValue: T {
        get { UserDefaults.standard.value(forKey: key) as! T }
        set { UserDefaults.standard.set(newValue, forKey: key) }
    }
    
    private func setBundleDefaults() {
        // Register the default values from Settings.bundle
        let settingsURL = Bundle.main.url(forResource: "Root", withExtension: "plist", subdirectory: "Settings.bundle")!
        let settingsRootDict = NSDictionary(contentsOf: settingsURL)!
        let prefSpecifiers = settingsRootDict["PreferenceSpecifiers"] as! [NSDictionary]
        
        let configurableSpecifiers = prefSpecifiers.filter { $0["Key"] != nil } // Not all settings items are meant to be read (like section titles, or groups)
        let keysAndValues: [(String, Any)] = configurableSpecifiers.map {
            ($0["Key"] as! String, $0["DefaultValue"]!)
        }
        
        UserDefaults.standard.register(defaults: Dictionary(uniqueKeysWithValues: keysAndValues))
    }
}

class SettingsService {
    
    static let observer = SettingsChangeObserver()
    
    static func initialize() {
        UserDefaults.standard.addObserver(
            observer,
            forKeyPath: "max_offline_storage",
            options: [.initial, .new],
            context: nil
        )
    }

    static func openAppSettings() {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
            GGLog.error("Unable to parse URL to open settings screen")
            return
        }
        
        if UIApplication.shared.canOpenURL(settingsUrl) {
            UIApplication.shared.open(settingsUrl)
        }
    }
}

class SettingsChangeObserver: NSObject {
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "max_offline_storage" {
            if let change = change {
                GGLog.info("User changed (or app was initialized with) 'max_offline_storage' to \(change[NSKeyValueChangeKey.init(rawValue: "new")] ?? "--error--") MB")
            }
            
            if UserState.isLoggedIn {
                DispatchQueue.global().async {
                    OfflineStorageService.purgeExtraTrackDataIfNeeded()
                }
            } else {
                GGLog.info("User was not logged in when offline storage handler was invoked. Not checking to purge extra tracks")
            }
        } else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
        }
    }
}

extension String {
    var boolValue: Bool {
        return (self as NSString).boolValue
    }
}
