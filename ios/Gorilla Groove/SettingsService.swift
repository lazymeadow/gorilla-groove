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
    
    static let observer = MyObserver()
    
    static func initialize() {
        UserDefaults.standard.addObserver(
            observer,
            forKeyPath: "max_offline_storage",
            options: [.new, .initial, .old],
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

class MyObserver: NSObject {
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "max_offline_storage" {
            GGLog.info("My change here1")

        } else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
        }
        
        GGLog.info("My change here2")
    }
}

extension String {
    var boolValue: Bool {
        return (self as NSString).boolValue
    }
}

extension UserDefaults {
    @objc dynamic var max_offline_storage: Int {
        return integer(forKey: "max_offline_storage")
    }
}
