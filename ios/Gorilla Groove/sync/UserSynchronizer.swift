import Foundation

class UserSynchronizer : StandardSynchronizer<User, UserResponse, UserDao> {
    
}

struct UserResponse: SyncResponseData {
    let id: Int
    let name: String
    let lastLogin: Date
    let createdAt: Date
    let updatedAt: Date
    
    func asEntity() -> Any {
        return User(
            id: id,
            name: name,
            lastLogin: lastLogin,
            createdAt: createdAt
        )
    }
}
