import Foundation
import UserNotifications

class NotificationService {
    static func showNotification() {
        if !areNotificationsEnabled() {
            requestNotifications()
            return
        }
        
        let content = UNMutableNotificationContent()
        content.title = "Feed the cat"
        content.subtitle = "It looks hungry"
//        content.sound = UNNotificationSound.default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.1, repeats: false)

        // choose a random identifier
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)

        // add our notification request
        UNUserNotificationCenter.current().add(request)
    }
    
    static func areNotificationsEnabled() -> Bool {
        let semaphore = DispatchSemaphore(value: 0)
        var enabled = false
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            enabled = settings.authorizationStatus == .authorized
            semaphore.signal()
        }
        
        semaphore.wait()
        return enabled
    }
    
    static func requestNotifications() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert]) { success, error in
            if success {
                GGLog.info("User accepted notification permission")
            } else if let error = error {
                GGLog.warning(error.localizedDescription)
            }
        }
    }
}
