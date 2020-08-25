import Foundation

public class Track : Entity {
    public var id: Int
    public var album: String
    public var artist: String
    public var createdAt: Date
    public var featuring: String?
    public var genre: String?
    public var isHidden: Bool
    public var isPrivate: Bool
    public var lastPlayed: Date?
    public var length: Int
    public var name: String
    public var note: String?
    public var playCount: Int
    public var releaseYear: Int?
    public var trackNumber: Int?
    public var userId: Int
    public var songCachedAt: Date?
    public var artCachedAt: Date?
    
    public init(
        id: Int,
        album: String,
        artist: String,
        createdAt: Date,
        featuring: String?,
        genre: String?,
        isHidden: Bool,
        isPrivate: Bool,
        lastPlayed: Date?,
        length: Int,
        name: String,
        note: String?,
        playCount: Int,
        releaseYear: Int?,
        trackNumber: Int?,
        userId: Int,
        songCachedAt: Date?,
        artCachedAt: Date?
    ) {
        self.id = id
        self.album = album
        self.artist = artist
        self.createdAt = createdAt
        self.featuring = featuring
        self.genre = genre
        self.isHidden = isHidden
        self.isPrivate = isPrivate
        self.lastPlayed = lastPlayed
        self.length = length
        self.name = name
        self.note = note
        self.playCount = playCount
        self.releaseYear = releaseYear
        self.trackNumber = trackNumber
        self.userId = userId
        self.songCachedAt = songCachedAt
        self.artCachedAt = artCachedAt
    }
    
    public static func fromDict(_ dict: [String : Any?]) -> Track {
        return Track(
            id: dict["id"] as! Int,
            album: dict["album"] as! String,
            artist: dict["artist"] as! String,
            createdAt: (dict["createdAt"] as! Int).toDate(),
            featuring: dict["featuring"] as! String?,
            genre: dict["genre"] as? String,
            isHidden: (dict["isHidden"] as! Int).toBool(),
            isPrivate: (dict["isPrivate"] as! Int).toBool(),
            lastPlayed: (dict["lastPlayed"] as? Int)?.toDate(),
            length: dict["length"] as! Int,
            name: dict["name"] as! String,
            note: dict["note"] as? String,
            playCount: dict["playCount"] as! Int,
            releaseYear: dict["releaseYear"] as? Int,
            trackNumber: dict["trackNumber"] as? Int,
            userId: dict["userId"] as! Int,
            songCachedAt: (dict["songCachedAt"] as? Int)?.toDate(),
            artCachedAt: (dict["artCachedAt"] as? Int)?.toDate()
        )
    }
}

public class TrackDao : BaseDao<Track> {
    static func getTracks(
        userId: Int,
        album: String? = nil,
        artist: String? = nil,
        sorts: Array<(String, Bool, Bool)> = []
    ) -> Array<Track> {
        let sortString = sorts.map { (key, isAscending, isNoCase) in
            key + (isNoCase ? " COLLATE NOCASE" : "") + (isAscending ? " ASC " : " DESC ")
        }.joined(separator: ",")
            
        let query = """
            SELECT *
            FROM track t
            WHERE user_id = \(userId)
            AND is_hidden = FALSE
            \(artist.asSqlParam("AND artist ="))
            \(album.asSqlParam("AND album ="))
            \(sortString.isEmpty ? "" : ("ORDER BY \(sortString)"))
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

    static func setCachedAt(trackId: Int, cachedAt: Date?, isSongCache: Bool) {
        let cacheString = cachedAt?.toEpochTime().toString() ?? "null"
        let cacheColumn = isSongCache ? "song_cached_at" : "art_cached_at"
        
        if !Database.execute("UPDATE track SET \(cacheColumn) = \(cacheString) WHERE id = \(trackId)") {
            fatalError("Failed to set cachedAt for track \(trackId)")
        }
    }
}

fileprivate extension Optional where Wrapped == String {
    func asSqlParam(_ sql: String) -> String {
        guard let string = self else {
            return ""
        }
        return sql + " '\(string.escaped())'"
    }
}

class CacheService {
    private static func baseDir() -> URL {
        return FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }
    
    static func getCachedSongData(_ trackId: Int) -> Data? {
        let path = baseDir().appendingPathComponent("\(trackId).mp3")
        
        return try? Data(contentsOf: path)
    }
    
    static func setCachedSongData(trackId: Int, data: Data) {
        let path = baseDir().appendingPathComponent("\(trackId).mp3")
        
        try! data.write(to: path)
        
        TrackDao.setCachedAt(trackId: trackId, cachedAt: Date(), isSongCache: true)
    }
    
    static func getCachedArtThumbnailData(_ trackId: Int) -> Data? {
        let path = baseDir().appendingPathComponent("\(trackId)-small.png")
        
        return try? Data(contentsOf: path)
    }
    
    static func setCachedArtThumbnailData(trackId: Int, data: Data) {
        let path = baseDir().appendingPathComponent("\(trackId)-small.png")
        
        try! data.write(to: path)
        
        TrackDao.setCachedAt(trackId: trackId, cachedAt: Date(), isSongCache: false)
    }
    
    static func deleteCachedSong(_ trackId: Int) {
        let path = baseDir().appendingPathComponent("\(trackId).mp3")

        deleteAtPath(path, trackId, "song")
    }
    
    static func deleteCachedArt(_ trackId: Int) {
        let path = baseDir().appendingPathComponent("\(trackId)-small.png")

        deleteAtPath(path, trackId, "art")
    }
    
    private static func deleteAtPath(_ path: URL, _ trackId: Int, _ itemDescription: String) {
        if FileManager.default.fileExists(atPath: path.absoluteString) {
            print("Deleting cached \(itemDescription) for track ID: \(trackId)")
            try! FileManager.default.removeItem(at: path)
        } else {
            print("Attempted to deleting cached \(itemDescription) for track ID: \(trackId) at path '\(path)' but it was not found")
        }
    }
}
