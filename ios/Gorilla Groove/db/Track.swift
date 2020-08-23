import Foundation

public struct Track : Entity {
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
    public var cachedAt: Date?
    
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
            cachedAt: (dict["cachedAt"] as? Int)?.toDate()
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
            SELECT id, album
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
                linkRequestLink: "file/link/\($0["id"] as! Int)?artSize=SMALL" // This is a stupid place to put a link, Ayrton
            )
        }
    }

    static func setCachedAt(trackId: Int, cachedAt: Date?) {
        let cacheString = cachedAt?.toEpochTime().toString() ?? "null"
        
        if !Database.execute("UPDATE track SET cached_at = \(cacheString) WHERE id = \(trackId)") {
            fatalError("Failed to set cachedAt for track \(trackId)")
        }
    }
}

fileprivate extension Optional where Wrapped == String {
    func asSqlParam(_ sql: String) -> String {
        guard let string = self else {
            return ""
        }
        return sql + " '\(string)'"
    }
}

extension Track {
    func getCachedSongData() -> Data? {
        let path = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("\(id).mp3")
        
        return try? Data(contentsOf: path)
    }
    
    static func setCachedSongData(trackId: Int, data: Data) {
        let documentDirectoryPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("\(trackId).mp3")
        
        try! data.write(to: documentDirectoryPath)
        
        TrackDao.setCachedAt(trackId: trackId, cachedAt: Date())
    }
}
