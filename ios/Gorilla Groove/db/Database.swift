import Foundation
import SQLite3

class Database {
    static var db: OpaquePointer?
    
    static func getDbPath() -> URL {
        let fileManager = FileManager.default
        let dbName = "Groove.sqlite"
        
        let documentsDirectoryURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        
        let persistentStoreURL = documentsDirectoryURL.appendingPathComponent(dbName)
        
        return persistentStoreURL
    }
    
    static func openDatabase() {
        let path = getDbPath().path
        
        let openResult = sqlite3_open(path, &db)
        if openResult == SQLITE_OK {
            print("Successfully opened connection to database at \(path)")
            
            migrate()
        } else {
            print("Unable to open database. Got result code \(openResult)")
        }
    }
    
    // Getting the last insert ID isn't thread safe in this function. Unsure if it matters
    static func execute(_ sql: String) -> Bool {
        var success: Bool = false
        var queryStatement: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &queryStatement, nil) == SQLITE_OK {
            success = sqlite3_step(queryStatement) == SQLITE_DONE
        } else {
            let errorMessage = String(cString: sqlite3_errmsg(db))
            print("Query is not prepared. Error: '\(errorMessage)'")
        }
        sqlite3_finalize(queryStatement)
        
        return success
    }
    
    static private func prepare(_ sql: String) -> OpaquePointer? {
        if db == nil {
            fatalError("Attempting to prepare a statement against a nil db!")
        }
        
        var statement: OpaquePointer? = nil
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else {
            print("Could not prepare statement: \(sql)")
            return nil
        }
        return statement
    }
    
    static func query(_ sql: String) -> [Dictionary<String, Any>] {
        var result: [Dictionary<String, Any>] = []
        
        guard let queryStatement = prepare(sql) else {
            return result
        }
        
        defer {
            sqlite3_finalize(queryStatement)
        }
        
        while sqlite3_step(queryStatement) == SQLITE_ROW {
            var dictionary: Dictionary<String, Any> = [:]
            
            for i in 0...sqlite3_column_count(queryStatement)-1 {
                let column: String = String(cString: sqlite3_column_name(queryStatement, i))
                let type = sqlite3_column_type(queryStatement, i)
                
                if type == SQLITE_TEXT, let result = sqlite3_column_text(queryStatement, i) {
                    dictionary[column] = String(cString: result)
                } else if type == SQLITE_INTEGER {
                    let result = sqlite3_column_int64(queryStatement, i) // This apparently can't be nil, but the text one can...?
                    dictionary[column] = Int(result)
                } else {
                    dictionary[column] = nil
                }
            }
            
            result.append(dictionary)
        }
        
        guard sqlite3_step(queryStatement) == SQLITE_DONE else {
            print("SQL execution did not end cleanly")
            return result
        }
        
        return result
    }
    
    static private func migrate() {
        let currentVersion = query("SELECT version FROM db_version")[safe: 0]?["version"] as? Int ?? 0
        
        print("Existing DB is using version: \(currentVersion)")
        
        if (currentVersion == 0) {
            print("Creating table user")
            var success = execute("""
        CREATE TABLE "user" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "name"    TEXT NOT NULL UNIQUE,
            "last_sync"    INTEGER,
            "last_login"    INTEGER NOT NULL,
            "created_at"    INTEGER NOT NULL
        );
        """)
            if !success {
                fatalError("Failed to create user table")
            }
            
            print("Creating table track")
            success = execute("""
        CREATE TABLE "track" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "user_id"    INTEGER NOT NULL,
            "name"    TEXT NOT NULL UNIQUE,
            "artist"    TEXT NOT NULL,
            "featuring"    TEXT NOT NULL,
            "album"    TEXT NOT NULL,
            "track_number"    INTEGER,
            "length"    INTEGER NOT NULL,
            "release_year"    INTEGER,
            "genre"    TEXT,
            "play_count"    INTEGER NOT NULL,
            "is_private"    INTEGER NOT NULL,
            "is_hidden"    INTEGER NOT NULL,
            "created_at"    INTEGER NOT NULL,
            "updated_at"    INTEGER NOT NULL,
            "last_played"    INTEGER NOT NULL,
            "note"    TEXT
        );
        """)
            if !success {
                fatalError("Failed to create track table")
            }
            
            print("Creating table playlist")
            success = execute("""
        CREATE TABLE "playlist" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "user_id"    INTEGER NOT NULL,
            "name"    TEXT NOT NULL,
            "created_at"    INTEGER NOT NULL,
            "updated_at"    INTEGER NOT NULL
        );
        """)
            if !success {
                fatalError("Failed to create playlist table")
            }
            
            print("Creating table playlist_track")
            success = execute("""
        CREATE TABLE "playlist_track" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "playlist_id"    INTEGER NOT NULL,
            "track_id"    INTEGER NOT NULL,
            "created_at"    INTEGER NOT NULL
        );
        """)
            if !success {
                fatalError("Failed to create playlist_track table")
            }
            
            print("Creating table db_version")
            success = execute("""
        CREATE TABLE "db_version" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "version"    INTEGER NOT NULL,
            "created_at"    INTEGER NOT NULL
        );
        """)
            if !success {
                fatalError("Failed to create db_version table")
            }
            
            success = execute("INSERT INTO db_version (version, created_at) VALUES (1, \(NSDate().timeIntervalSince1970))")
            if !success {
                fatalError("Failed to update db_version!")
            }
        }
    }
}


extension Collection {
    
    /// Returns the element at the specified index if it is within bounds, otherwise nil.
    subscript (safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

