import Foundation

public struct PlaylistTrack : Entity {
    public var id: Int
    public var playlistId: Int
    public var sortOrder: Int
    public var createdAt: Date
    public var trackId: Int
    
    public static func fromDict(_ dict: [String : Any?]) -> PlaylistTrack {
        return PlaylistTrack(
            id: dict["id"] as! Int,
            playlistId: dict["playlistId"] as! Int,
            sortOrder: dict["sortOrder"] as! Int,
            createdAt: (dict["createdAt"] as! Int).toDate(),
            trackId: dict["trackId"] as! Int
        )
    }
}

public class PlaylistTrackDao : BaseDao<PlaylistTrack> {
    static func findByPlaylistAndTrack(
        playlistId: Int,
        trackId: Int
    ) -> [PlaylistTrack] {
        return queryEntities("""
            SELECT pt.*
            FROM playlist_track pt
            WHERE pt.playlist_id = \(playlistId)
            AND pt.track_id = \(trackId)
        """)
    }
    
    static func getPlaylistTracksForPlaylist(
        _ playlistId: Int,
        isSongCached: Bool? = nil
    ) -> [PlaylistTrack] {
        var isCachedQuery = ""
        if let isSongCached = isSongCached {
            isCachedQuery = "AND (t.song_cached_at IS \(isSongCached ? "NOT" : "") NULL)"
        }
        
        return queryEntities("""
            SELECT pt.*
            FROM playlist_track pt
            JOIN track t
                ON pt.track_id = t.id
            LEFT JOIN playlist p
                ON pt.playlist_id = p.id
            WHERE p.id = \(playlistId)
            \(isCachedQuery)
            ORDER BY pt.sort_order ASC
        """)
    }
    
    static func setSortOrderForPlaylistTrackId(_ playlistTrackId: Int, sortOrder: Int) {
        if !Database.execute("UPDATE playlist_track SET sort_order = \(sortOrder) WHERE id = \(playlistTrackId)") {
            GGLog.critical("Failed to update sortOrder for playlistTrack \(playlistTrackId)")
        }
    }
}
