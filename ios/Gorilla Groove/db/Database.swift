import Foundation
import SQLite3

class Database {
    static var db: OpaquePointer?
    
    private static let logger = GGLogger(category: "db")
    
    static func getDbPath(_ userId: Int) -> URL {
        let dbName = "Groove-\(userId).sqlite"
        
        let basePath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return basePath.appendingPathComponent(dbName)
    }
    
    static func openDatabase(userId: Int) {
        let path = getDbPath(userId).path
        
        let openResult = sqlite3_open_v2(path, &db, SQLITE_OPEN_READWRITE | SQLITE_OPEN_FULLMUTEX | SQLITE_OPEN_CREATE, nil)
        if openResult == SQLITE_OK {
            logger.info("Successfully opened connection to database at \(path)")
            
            migrate()
            
            if (getDbVersion() == 4) {
                logger.info("User is on a flawed DB version and needs a forced migration")
                reset(userId)
            }
        } else {
            logger.critical("Unable to open database. Got result code \(openResult)")
        }
    }
    
    static func close() {
        sqlite3_close(db)
    }
    
    static func reset(_ userId: Int) {
        try! FileManager.default.removeItem(at: getDbPath(userId))
        openDatabase(userId: userId)
    }
    
    // Getting the last insert ID isn't thread safe in this function. Unsure if it matters
    static func execute(_ sql: String) -> Bool {
        var success: Bool = false
        var queryStatement: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, SQLITE_OPEN_READWRITE | SQLITE_OPEN_FULLMUTEX | SQLITE_OPEN_CREATE, &queryStatement, nil) == SQLITE_OK {
            success = sqlite3_step(queryStatement) == SQLITE_DONE
        } else {
            let errorMessage = String(cString: sqlite3_errmsg(db))
            logger.critical("Query is not prepared. Error: '\(errorMessage)'")
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
            logger.critical("Could not prepare statement: \(sql)")
            return nil
        }
        return statement
    }
    
    static func query(_ sql: String) -> [Dictionary<String, Any?>] {
        var result: [Dictionary<String, Any?>] = []
        
        guard let queryStatement = prepare(sql) else {
            return result
        }
        
        defer {
            sqlite3_finalize(queryStatement)
        }
        
        var currentStep = sqlite3_step(queryStatement)
        while currentStep == SQLITE_ROW {
            var dictionary: Dictionary<String, Any?> = [:]
            
            for i in 0...sqlite3_column_count(queryStatement)-1 {
                let column: String = String(cString: sqlite3_column_name(queryStatement, i))
                let type = sqlite3_column_type(queryStatement, i)
                
                if type == SQLITE_TEXT, let result = sqlite3_column_text(queryStatement, i) {
                    dictionary[column] = String(cString: result)
                } else if type == SQLITE_INTEGER {
                    let result = sqlite3_column_int64(queryStatement, i) // This apparently can't be nil, but the text one can...?
                    dictionary[column] = Int(result)
                } else if type == SQLITE_FLOAT {
                    let result = sqlite3_column_double(queryStatement, i)
                    dictionary[column] = Double(result)
                } else {
                    dictionary[column] = nil
                }
            }
            
            result.append(dictionary)
            currentStep = sqlite3_step(queryStatement)
        }
        
        guard currentStep == SQLITE_DONE else {
            if currentStep == SQLITE_MISUSE {
                logger.critical("SQL execution was misused!")
            } else {
                logger.critical("SQL execution did not end cleanly with code: \(currentStep)")
            }
            return result
        }
        
        return result
    }
    
    static func getDbVersion() -> Int {
        return query("SELECT version FROM db_version")[safe: 0]?["version"] as? Int ?? 0
    }
    
    static private func migrate() {
        let currentVersion = getDbVersion()
        let targetVersion = 7
        
        logger.info("Existing DB is using version: \(currentVersion)")

        if currentVersion == targetVersion {
            logger.debug("Target DB version is: \(targetVersion). Skipping migrate")
            return
        }
        
        if (currentVersion == 0) {
            logger.info("Creating table user")
            executeOrFail("""
        CREATE TABLE "user" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "name"    TEXT NOT NULL UNIQUE,
            "last_sync"    INTEGER,
            "last_login"    INTEGER NOT NULL,
            "created_at"    INTEGER NOT NULL
        );
        """)
            
            logger.info("Creating table track")
            executeOrFail("""
        CREATE TABLE "track" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "user_id"    INTEGER NOT NULL,
            "name"    TEXT NOT NULL,
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
            "added_to_library"    INTEGER,
            "last_played"    INTEGER,
            "in_review"    INTEGER NOT NULL,
            "note"    TEXT
        );
        """)

            logger.info("Creating table playlist")
            executeOrFail("""
        CREATE TABLE "playlist" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "user_id"    INTEGER NOT NULL,
            "name"    TEXT NOT NULL,
            "created_at"    INTEGER NOT NULL,
            "updated_at"    INTEGER NOT NULL
        );
        """)

            logger.info("Creating table playlist_track")
            executeOrFail("""
        CREATE TABLE "playlist_track" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "playlist_id"    INTEGER NOT NULL,
            "track_id"    INTEGER NOT NULL,
            "created_at"    INTEGER NOT NULL
        );
        """)

            logger.info("Creating table db_version")
            executeOrFail("""
        CREATE TABLE "db_version" (
            "id"    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
            "version"    INTEGER NOT NULL,
            "created_at"    INTEGER NOT NULL
        );
        """)

            executeOrFail("INSERT INTO db_version (version, created_at) VALUES (1, \(Date().toEpochTime()))")
        }
        
        if currentVersion < 2 {
            logger.info("Adding cached_at column")
            executeOrFail("ALTER TABLE track ADD cached_at INT NULL;")
        }
        
        if currentVersion < 3 {
            logger.info("Renaming song cache column")
            executeOrFail("ALTER TABLE track RENAME cached_at TO song_cached_at;")
        }
        
        if currentVersion < 4 {
            logger.info("Adding art cache column")
            executeOrFail("ALTER TABLE track ADD art_cached_at INT NULL;")
        }
        
        // 5 was skipped as there was a bug
        
        if currentVersion < 6 {
            logger.info("Adding track offline availability column")
            executeOrFail("ALTER TABLE track ADD offline_availability TEXT NOT NULL DEFAULT 'NORMAL';")
        }
        
        if currentVersion < 7 {
            logger.info("Adding storage sizes and thumbnail cache columns")
            executeOrFail("ALTER TABLE track ADD filesize_song_ogg INT NOT NULL DEFAULT 0;")
            executeOrFail("ALTER TABLE track ADD filesize_song_mp3 INT NOT NULL DEFAULT 0;")
            executeOrFail("ALTER TABLE track ADD filesize_art_png INT NOT NULL DEFAULT 0;")
            executeOrFail("ALTER TABLE track ADD filesize_thumbnail_png INT NOT NULL DEFAULT 0;")
            executeOrFail("ALTER TABLE track ADD thumbnail_cached_at INT NULL;")
            
            // I previously used the art_cached_at to mean thumbnail data. Not the smartest, really. Now that I want to
            // actually cache full sized thumbnail art, I am migrating that data to a new column specifically for thumbnails.
            executeOrFail("UPDATE track SET thumbnail_cached_at = art_cached_at;")
            executeOrFail("UPDATE track SET art_cached_at = NULL;")
        }
        
        executeOrFail("UPDATE db_version SET version = \(targetVersion)")
        logger.info("Datbase was upgraded to version \(targetVersion)")
    }
    
    private static func executeOrFail(_ sql: String) {
        let success = execute(sql)
        if !success {
            let errorMessage = "Failed to execute sql: \(sql)"
            GGLog.critical(errorMessage)
            fatalError(errorMessage)
        }
    }
}

extension Collection {
    
    /// Returns the element at the specified index if it is within bounds, otherwise nil.
    subscript (safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

