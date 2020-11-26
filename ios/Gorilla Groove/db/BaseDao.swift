import Foundation

public class BaseDao<T: Entity> {
    static func save(_ entity: T) {
        let mirror = Mirror(reflecting: entity)
        
        var columnNames = [String]()
        var columnValues = [String]()
        let tableName = String(describing: T.self).toSnakeCase()
        
        for child in mirror.children {
            // Skip the ID column if ID is 0. This is a new insert and we can omit it from the SQL and have it auto-increment
            if (child.label! == "id" && child.value as! Int == 0) {
                continue
            }
            
            // DB names are stored snake_case because reasons. So convert them
            columnNames.append(child.label!.toSnakeCase())
            
            let value = child.value
            
            if value is Bool {
                let boolType = value as! Bool
                columnValues.append(boolType ? "1" : "0")
            } else if value is String {
                let stringType = value as! String
                columnValues.append("'\(stringType.escaped())'")
            } else if value is Int {
                let intType = value as! Int
                columnValues.append(String(intType))
            } else if value is Date {
                let dateType = value as! Date
                columnValues.append(String(dateType.toEpochTime()))
            } else if value is DbEnum {
                let enumType = value as! DbEnum
                columnValues.append("'\(enumType.getDbName())\'")
            } else {
                // This is incredibly stupid, but optionals from Mirror seem batshit crazy. Have to check for nil like this.
                // StackOverflow suggests not using Mirror and instead using the objc runtime because Mirror sucks. Thanks Apple.
                // https://stackoverflow.com/questions/50254646/how-to-check-if-anynot-any-is-nil-or-not-in-swift
                if case Optional<Any>.none = value {
                    columnValues.append("null")
                } else {
                    fatalError("Unsupported type tried to be saved! \(type(of: child.value))")
                }
            }
        }
                
        let query = """
            REPLACE INTO \(tableName) (\(columnNames.joined(separator: ",")))
            VALUES (\(columnValues.joined(separator: ",")))
        """
        let success = Database.execute(query)
        
        if !success {
            fatalError("Faild to save entity! Failed query:\n" + query)
        }
    }
    
    static func findById(_ id: Int) -> T? {
        // DB names are stored snake_case because reasons. So convert them
        let tableName = String(describing: T.self).toSnakeCase()
        
        let result = Database.query("SELECT * FROM \(tableName) WHERE id = \(id)")
        if result.isEmpty {
            return nil
        } else {
            return dictToEntity(result.first!)
        }
    }
    
    static func dictToEntity(_ dict: [String: Any?]) -> T {
        let structData = dict.reduce(into: [:]) { result, x in
            result[x.key.toCamelCase()] = x.value
        }
    
        return T.fromDict(structData) as! T
    }
    
    static func delete(_ entity: T) {
        delete(entity.id)
    }
    
    static func delete(_ entityId: Int) {
        let tableName = String(describing: T.self).toSnakeCase()

        let success = Database.execute("DELETE FROM \(tableName) WHERE id = \(entityId)")
        if !success {
            fatalError("Failed to delete entity \(tableName) with ID \(entityId)")
        }
    }
    
    static func queryEntities(_ query: String) -> Array<T> {
        let users = Database.query(query)
        
        return users.map { dictToEntity($0) }
    }
}

extension String {
    func toCamelCase() -> String {
        return self.lowercased()
            .split(separator: "_")
            .enumerated()
            .map { $0.offset > 0 ? $0.element.capitalized : $0.element.lowercased() }
            .joined()
    }
    
    func toSnakeCase() -> String {
        return unicodeScalars.reduce("") {
            if CharacterSet.uppercaseLetters.contains($1) {
                if $0.count > 0 {
                    return ($0 + "_" + String($1))
                }
            }
            return $0 + String($1)
        }.lowercased()
    }
}


public protocol Entity : Decodable {
    associatedtype T
    var id: Int { get set }
    
    static func fromDict(_ dict: [String: Any?]) -> T
}

extension Int {
    func toBool() -> Bool {
        switch self {
        case 0:
            return false
        case 1:
            return true
        default:
            fatalError("Non-boolean integer value! \(self)")
        }
    }
    
    func toDate() -> Date {
        return Date(timeIntervalSince1970: Double(self) / 1000.0)
    }
    
    func toString() -> String {
        return String(self)
    }
}

extension Date {
    func toEpochTime() -> Int {
        return (Int(self.timeIntervalSince1970 * 1000))
    }
}

extension String {
    func escaped() -> String {
        return self.replacingOccurrences(of: "'", with: "''")
    }
}
