import Foundation

public struct User : Entity {
    public typealias T = User
    
    public var id: Int
    public var name: String
    public var lastLogin: Date?
    public var createdAt: Date
    
    public static func fromDict(_ dict: [String : Any?]) -> User {
        return User(
            id: dict["id"] as! Int,
            name: dict["name"] as! String,
            lastLogin: (dict["lastLogin"] as? Int)?.toDate(),
            createdAt: (dict["createdAt"] as! Int).toDate()
        )
    }
}

public class UserDao : BaseDao<User> {
    static func getOtherUsers() -> Array<User> {
        let ownId = FileState.read(LoginState.self)!.id

        return queryEntities("SELECT * FROM user WHERE id != \(ownId) ORDER BY name COLLATE NOCASE ASC")
    }
}
