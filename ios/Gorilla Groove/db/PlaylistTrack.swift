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
}
