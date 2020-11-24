import Foundation
import CoreLocation
import UIKit
import os

class LocationService {
    private static var locationManager: CLLocationManager = CLLocationManager()
    private static let delegate = LocationManagerDelegate()
    private static var authorized = false
    private static var authorizationChecked = false
    private static var bootPermissionChecked = false
    
    private static var locationRequestSemaphore: DispatchSemaphore? = nil
    
    private static let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "location")
    
    @SettingsBundleStorage(key: "location_min_battery")
    private static var locationMinBattery: Int
    
    @SettingsBundleStorage(key: "location_enabled")
    private static var locationEnabled: Bool
    
    static func getLocationPoint() -> CLLocation? {
        if Thread.isMainThread {
            fatalError("Don't check location on the main thread")
        }
        
        if !authorized {
            logger.info("Not authorized to gather location points")
            return nil
        }
        
        if ProcessInfo.processInfo.isLowPowerModeEnabled {
            logger.info("Low power mode was enabled. Not gathering location point")
            return nil
        }
        
        if !locationEnabled {
            logger.info("Location was not enabled. Not gathering location point")
            return nil
        }
        
        let minBattery = locationMinBattery // Accessing this hits prefs so I'm caching it as a local variable
        if minBattery > 0 {
            let batteryLevel = Int(UIDevice.current.batteryLevel * 100)
            if batteryLevel < minBattery {
                if UIDevice.current.batteryState == .charging {
                    logger.info("Battery was lower than the minimum to gather location, but was plugged in. Minimum: \(minBattery). Had: \(batteryLevel)")
                } else {
                    logger.info("Battery was too low to gather a location point and was not plugged in. Minimum: \(minBattery). Had: \(batteryLevel)")
                    return nil
                }
            } else if minBattery == -100 {
                logger.info("Battery monitoring was not enabled. This is an error. Ignoring battery check")
            }
        }
        
        // Though I don't know why it would ever happen, in theory two threads could both request location more or less at the same time.
        // If that happens, don't bother requesting another location. Just wait on the first thread's request and both take the point.
        if locationRequestSemaphore == nil {
            locationRequestSemaphore = DispatchSemaphore(value: 0)
            locationManager.requestLocation()
        } else {
            logger.warning("A thread was requesting location while another one was waiting on it. Is this expected?")
        }

        locationRequestSemaphore?.wait()
        locationRequestSemaphore = nil
        guard let location = locationManager.location else {
            logger.error("No location point could be gathered!")
            return nil
        }
        
        return location
    }
    
    static func initialize() {
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.delegate = delegate
    }
    
    static func requestLocationPermissionIfNeeded() {
        if authorizationChecked {
            return
        }
        
        // These iOS 14 checks are pointless. The minimum deployment target is 14.0. I have no idea why xcode is requiring this bullshit
        if #available(iOS 14.0, *) {
            if locationManager.authorizationStatus == .notDetermined {
                locationManager.requestAlwaysAuthorization()
            }
        }
    }
    
    class LocationManagerDelegate: NSObject, CLLocationManagerDelegate {
        func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
            if #available(iOS 14.0, *) {
                let status = manager.authorizationStatus
                authorizationChecked = true

                switch status {
                case .notDetermined:
                    logger.info("Location permission is not determined")
                    authorized = false
                    authorizationChecked = false
                case .restricted, .denied:
                    logger.info("Location permission is denied")
                    authorized = false
                case .authorizedWhenInUse:
                    logger.info("Location permission is granted as 'When In Use'")
                    authorized = true
                    // This delegate function is triggered on app open, and we don't want to pop this modal open every time someone opens the app.
                    // Only show it if they changed permissions to this SINCE opening up the app
                    if bootPermissionChecked {
                        ViewUtil.showAlert(
                            title: "Data will be incomplete",
                            message: "Your location permission is set to 'While Using the App'. Gorilla Groove will be unable to read your location while listening to music with the app in the background. Would you like to change location to 'Always'?",
                            yesText: "Yes"
                        ) { ViewUtil.openAppSettings() }
                    }
                case .authorizedAlways:
                    logger.info("Location permission is granted as 'Always'")
                    authorized = true
                default:
                    logger.info("Unknown permission grant type encountered")
                    authorized = false
                }
                
                // If we're good to go on checking for location, then enable battery monitoring since we will use battery levels
                // to determine if we should pull location points
                if authorized && !UIDevice.current.isBatteryMonitoringEnabled {
                    UIDevice.current.isBatteryMonitoringEnabled = true
                }
                bootPermissionChecked = true
            }
        }
        
        func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
            if let location = locations.first {
                logger.info("Found user's location: \(location)")
            } else {
                logger.error("Found no location??")
            }
            locationRequestSemaphore?.signal()
        }

        func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
            logger.error("Failed to find user's location: \(error.localizedDescription)")
            locationRequestSemaphore?.signal()
        }
    }
}
