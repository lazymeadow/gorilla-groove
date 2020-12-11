import Foundation

public class Track : Entity {
    public var id: Int
    public var album: String
    public var artist: String
    public var addedToLibrary: Date?
    public var featuring: String?
    public var genre: String?
    public var isHidden: Bool
    public var isPrivate: Bool
    public var inReview: Bool
    public var lastPlayed: Date?
    public var startedOnDevice: Date?
    public var length: Int
    public var name: String
    public var note: String?
    public var playCount: Int
    public var releaseYear: Int?
    public var trackNumber: Int?
    public var userId: Int
    public var songCachedAt: Date?
    public var artCachedAt: Date?
    public var thumbnailCachedAt: Date?
    public var offlineAvailability: OfflineAvailabilityType
    public var filesizeSongOgg: Int
    public var filesizeSongMp3: Int
    public var filesizeArtPng: Int
    public var filesizeThumbnailPng: Int
    
    public var cacheSize: Int {
        get {
            // Thumbnails are cached separately from the rest of the song data, and are tiny anyway. So don't include in this calculation.
            return filesizeSongMp3 + filesizeArtPng
        }
    }

    public init(
        id: Int,
        album: String,
        artist: String,
        addedToLibrary: Date?,
        featuring: String?,
        genre: String?,
        isHidden: Bool,
        isPrivate: Bool,
        inReview: Bool,
        lastPlayed: Date?,
        startedOnDevice: Date?,
        length: Int,
        name: String,
        note: String?,
        playCount: Int,
        releaseYear: Int?,
        trackNumber: Int?,
        userId: Int,
        songCachedAt: Date?,
        artCachedAt: Date?,
        thumbnailCachedAt: Date?,
        offlineAvailability: OfflineAvailabilityType,
        filesizeSongOgg: Int,
        filesizeSongMp3: Int,
        filesizeArtPng: Int,
        filesizeThumbnailPng: Int
    ) {
        self.id = id
        self.album = album
        self.artist = artist
        self.addedToLibrary = addedToLibrary
        self.featuring = featuring
        self.genre = genre
        self.isHidden = isHidden
        self.isPrivate = isPrivate
        self.inReview = inReview
        self.lastPlayed = lastPlayed
        self.startedOnDevice = startedOnDevice
        self.length = length
        self.name = name
        self.note = note
        self.playCount = playCount
        self.releaseYear = releaseYear
        self.trackNumber = trackNumber
        self.userId = userId
        self.songCachedAt = songCachedAt
        self.artCachedAt = artCachedAt
        self.thumbnailCachedAt = thumbnailCachedAt
        self.offlineAvailability = offlineAvailability
        self.filesizeSongOgg = filesizeSongOgg
        self.filesizeSongMp3 = filesizeSongMp3
        self.filesizeArtPng = filesizeArtPng
        self.filesizeThumbnailPng = filesizeThumbnailPng
    }
    
    public static func fromDict(_ dict: [String : Any?]) -> Track {
        return Track(
            id: dict["id"] as! Int,
            album: dict["album"] as! String,
            artist: dict["artist"] as! String,
            addedToLibrary: (dict["addedToLibrary"] as? Int)?.toDate(),
            featuring: dict["featuring"] as! String?,
            genre: dict["genre"] as? String,
            isHidden: (dict["isHidden"] as! Int).toBool(),
            isPrivate: (dict["isPrivate"] as! Int).toBool(),
            inReview: (dict["inReview"] as! Int).toBool(),
            lastPlayed: (dict["lastPlayed"] as? Int)?.toDate(),
            startedOnDevice: (dict["startedOnDevice"] as? Int)?.toDate(),
            length: dict["length"] as! Int,
            name: dict["name"] as! String,
            note: dict["note"] as? String,
            playCount: dict["playCount"] as! Int,
            releaseYear: dict["releaseYear"] as? Int,
            trackNumber: dict["trackNumber"] as? Int,
            userId: dict["userId"] as! Int,
            songCachedAt: (dict["songCachedAt"] as? Int)?.toDate(),
            artCachedAt: (dict["artCachedAt"] as? Int)?.toDate(),
            thumbnailCachedAt: (dict["thumbnailCachedAt"] as? Int)?.toDate(),
            offlineAvailability: OfflineAvailabilityType(rawValue: (dict["offlineAvailability"] as! String)) ?? OfflineAvailabilityType.UNKNOWN,
            filesizeSongOgg: dict["filesizeSongOgg"] as! Int,
            filesizeSongMp3: dict["filesizeSongMp3"] as! Int,
            filesizeArtPng: dict["filesizeArtPng"] as! Int,
            filesizeThumbnailPng: dict["filesizeThumbnailPng"] as! Int
        )
    }
}

public class TrackDao : BaseDao<Track> {
    static func getTracks(
        userId: Int,
        album: String? = nil,
        artist: String? = nil,
        isHidden: Bool? = nil,
        inReview: Bool = false,
        offlineAvailability: OfflineAvailabilityType? = nil,
        isCached: Bool? = nil,
        isSongCached: Bool? = nil,
        sorts: Array<(String, Bool, Bool)> = [],
        limit: Int? = nil
    ) -> Array<Track> {
        let sortString = sorts.map { (key, isAscending, isNoCase) in
            key + (isNoCase ? " COLLATE NOCASE" : "") + (isAscending ? " ASC " : " DESC ")
        }.joined(separator: ",")
        
        // If the song or art is cached, the track is considered cached. A track is not considered cached if these
        // things don't exist, but COULD exist (so if art doesn't exist, but there is no art to fetch, it doesn't need caching).
        // So it is possible for a song to be returned from "isCached" being true OR false, if one thing is cached but not the other.
        var isCachedQuery = ""
        if let isSongCached = isSongCached {
            isCachedQuery = "AND (song_cached_at IS \(isSongCached ? "NOT" : "") NULL)"
        } else if let isCached = isCached {
            if isCached {
                isCachedQuery = "AND (song_cached_at IS NOT NULL OR art_cached_at IS NOT NULL)"
            } else {
                isCachedQuery = "AND (song_cached_at IS NULL OR (art_cached_at IS NULL AND filesize_art_png > 0))"
            }
        }
        
        let query = """
            SELECT *
            FROM track t
            WHERE user_id = \(userId)
            \(isHidden.asSqlParam("AND is_hidden ="))
            AND in_review = \(inReview)
            \(artist.asSqlParam("AND artist ="))
            \(album.asSqlParam("AND album ="))
            \(isCachedQuery)
            \(offlineAvailability?.getDbName().asSqlParam("AND offline_availability =") ?? "")
            \(sortString.isEmpty ? "" : ("ORDER BY \(sortString)"))
            \(limit.asSqlParam("LIMIT"))
        """
        
        return queryEntities(query)
    }
    
    static func getTracksForPlaylist(_ playlistId: Int) -> Array<Track> {
        return queryEntities("""
            SELECT t.*
            FROM track t
            LEFT JOIN playlist_track pt
                ON pt.track_id = t.id
            LEFT JOIN playlist p
                ON pt.playlist_id = p.id
            WHERE p.id = \(playlistId)
        """)
    }
    
    static func getArtists(userId: Int) -> Array<String> {
        let artistRows = Database.query("""
            SELECT artist
            FROM track
            WHERE user_id = \(userId)
            AND is_hidden = FALSE
            GROUP BY artist COLLATE NOCASE
            ORDER BY artist COLLATE NOCASE ASC
        """)
        
        return artistRows.map { $0["artist"] as! String }
    }
    
    static func getAlbums(userId: Int, artist: String? = nil) -> Array<Album> {
        let artistRows = Database.query("""
            SELECT id, album, art_cached_at
            FROM track
            WHERE user_id = \(userId)
            AND is_hidden = FALSE
            \(artist.asSqlParam("AND artist ="))
            GROUP BY album
            ORDER BY album COLLATE NOCASE ASC
        """)
        
        return artistRows.map {
            // Doesn't matter what track ID we get. Just need one of them so we can get the album art for the track (and just assume it's all the same)
            Album(
                name: $0["album"] as! String,
                trackIdForArt: $0["id"] as! Int,
                artCached: $0["art_cached_at"] != nil
            )
        }
    }

    static func setCachedAt(trackId: Int, cachedAt: Date?, cacheType: CacheType) {
        let cacheString = cachedAt?.toEpochTime().toString() ?? "null"
        let cacheColumn: String
        
        switch (cacheType) {
        case .art:
            cacheColumn = "art_cached_at"
        case .song:
            cacheColumn = "song_cached_at"
        case .thumbnail:
            cacheColumn = "thumbnail_cached_at"
        }
        
        if !Database.execute("UPDATE track SET \(cacheColumn) = \(cacheString) WHERE id = \(trackId)") {
            GGLog.critical("Failed to set cachedAt for track \(trackId)")
        }
    }
    
    static func setDevicePlayStart(trackId: Int, date: Date?) {
        let playedString = date?.toEpochTime().toString() ?? "null"

        if !Database.execute("UPDATE track SET started_on_device = \(playedString) WHERE id = \(trackId)") {
            GGLog.critical("Failed to set startedOnDevice for track \(trackId)!")
        }
    }
    
    // Total bytes of the MP3 and the full sized art for songs that are cached.
    // Album art thumbnails are not taken into account as it is downloaded when it is viewed, rather than when a song is played. And it's small.
    static func getTotalBytesStored() -> Int {
        // Need to COALESCE these queries, as having nothing cached returns null and not 0 from SUM()
        let songBytes = Database.query("""
            SELECT COALESCE(SUM(filesize_song_mp3), 0) as total
            FROM track
            WHERE song_cached_at IS NOT NULL
        """)[0]["total"] as! Int
        
        let artBytes = Database.query("""
            SELECT COALESCE(SUM(filesize_art_png), 0) as total
            FROM track
            WHERE art_cached_at IS NOT NULL
        """)[0]["total"] as! Int
        
        return songBytes + artBytes
    }
    
    static func getOfflineAvailabilityCounts(cachedOnly: Bool) -> [OfflineAvailabilityType: Int] {
        let cachedOnlyQuery = cachedOnly ? "WHERE song_cached_at IS NOT NULL" : ""
        var counts: [OfflineAvailabilityType: Int] = [:]
        // Need to COALESCE these queries, as having nothing cached returns null and not 0 from SUM()
        Database.query("""
            SELECT offline_availability, count(*) AS total
            FROM track
            \(cachedOnlyQuery)
            GROUP BY offline_availability
        """).forEach { entry in
            let availabilityType = OfflineAvailabilityType(rawValue: (entry["offline_availability"] as! String)) ?? OfflineAvailabilityType.UNKNOWN
            counts[availabilityType] = entry["total"] as? Int ?? 0
        }
        
        OfflineAvailabilityType.allCases.forEach { type in
            if counts[type] == nil {
               counts[type] = 0
            }
        }

        return counts
    }
    
    static func getOfflineAvailabilityMaxStorage() -> [OfflineAvailabilityType: Int] {
        var storageRequired: [OfflineAvailabilityType: Int] = [:]
        
        Database.query("""
            SELECT offline_availability, sum(filesize_song_mp3 + filesize_art_png) as total
            FROM track
            GROUP BY offline_availability
        """).forEach { entry in
            let availabilityType = OfflineAvailabilityType(rawValue: (entry["offline_availability"] as! String)) ?? OfflineAvailabilityType.UNKNOWN
            storageRequired[availabilityType] = entry["total"] as? Int ?? 0
        }
        
        OfflineAvailabilityType.allCases.forEach { type in
            if storageRequired[type] == nil {
                storageRequired[type] = 0
            }
        }
        
        return storageRequired
    }
    
}

fileprivate extension Optional where Wrapped == Int {
    func asSqlParam(_ sql: String) -> String {
        guard let intVal = self else {
            return ""
        }
        return String(intVal).asSqlParam(sql)
    }
}

fileprivate extension Optional where Wrapped == String {
    func asSqlParam(_ sql: String) -> String {
        guard let int = self else {
            return ""
        }
        return sql + " \(int)"
    }
}

fileprivate extension Optional where Wrapped == Bool {
    func asSqlParam(_ sql: String) -> String {
        guard let bool = self else {
            return ""
        }
        return sql + " \(bool)"
    }
}

fileprivate extension String {
    func asSqlParam(_ sql: String) -> String {
        return sql + " '\(self.escaped())'"
    }
}

extension Optional where Wrapped == Bool {
  func toString() -> String? {
    self.map { String($0) }
  }
}

public enum OfflineAvailabilityType: String, Codable, DbEnum, CaseIterable {
    case NORMAL
    case AVAILABLE_OFFLINE
    case ONLINE_ONLY
    case UNKNOWN // Future API additions may not yet be mapped
    
    func getDbName() -> String {
        return rawValue
    }
}

// Swift reflection is really bad. Can't check if something is just an 'Enum'. Can't check if something
// is 'RawRepresentable'. We CAN check if something is 'Encodable', but hilariously, JSONEncoder can't
// take a generic Encodable, it has to be a concrete type. So my hands are tied and every enum now has
// to have a "getDbName" function that has the exact same implementation. Thanks Apple.
protocol DbEnum {
    func getDbName() -> String
}
