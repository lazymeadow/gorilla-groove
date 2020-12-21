import Foundation
import CoreData

class UserState {
    
    static var isLoggedIn: Bool = false
    
    static func getOwnUser() -> User {
        let ownId = FileState.read(LoginState.self)!.id
        
        if let user = UserDao.findById(ownId) {
            return user
        }

        // Bit of an edge case here. Just put fake defaults in for everything but the ID. If we just created this user
        // it means we're about to do our first sync and this will all get updated anyway
        let newUser = User(id: ownId, name: "", lastLogin: Date(), createdAt: Date())
        UserDao.save(newUser)
        
        return newUser
    }
    
    static func postCurrentDevice(
        onComplete: @escaping () -> Void
    ) {
        let requestBody = PostSessionRequest(
            deviceId: FileState.read(DeviceState.self)!.deviceId,
            version: AppDelegate.getAppVersion()
        )
        
        HttpRequester.put("device", EmptyResponse.self, requestBody) { _, responseCode, _ in
            if (responseCode < 200 || responseCode >= 300) {
                GGLog.error("Failed to inform the server of our current device!")
            } else {
                GGLog.info("Posted current device to server")
            }
            
            onComplete()
        }
    }
    
    struct PostSessionRequest: Encodable {
        let deviceId: String
        let deviceType: String = "IPHONE"
        let version: String
    }
}
