import Foundation

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

extension String {
    var boolValue: Bool {
        return (self as NSString).boolValue
    }
}
