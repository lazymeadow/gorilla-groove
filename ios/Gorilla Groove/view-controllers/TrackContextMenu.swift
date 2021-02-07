import Foundation
import UIKit

class TrackContextMenu {
    static func createMenuForTrack(
        _ track: Track,
        view: TrackContextView,
        playlist: Playlist?,
        parentVc: UIViewController
    ) -> UIAlertController {
        let alert = GGActionSheet.create()
        
        alert.addAction(UIAlertAction(title: "Play Next", style: .default, handler: { _ in
            NowPlayingTracks.addTrackNext(track)
        }))
        alert.addAction(UIAlertAction(title: "Play Last", style: .default, handler: { _ in
            NowPlayingTracks.addTrackLast(track)
        }))
        
        if view == .MY_LIBRARY || view == .NOW_PLAYING {
            alert.addAction(UIAlertAction(title: "Edit Properties", style: .default, handler: { _ in
                let metadataController = EditMetadataController(track: track)
                metadataController.modalPresentationStyle = .pageSheet
                
                let vc = UINavigationController(rootViewController: metadataController)
                parentVc.present(vc, animated: true)
            }))
            
            alert.addAction(UIAlertAction(title: "Recommend", style: .default, handler: { _ in
                let usersController = SelectUsersController(track)
                usersController.modalPresentationStyle = .pageSheet
                
                let vc = UINavigationController(rootViewController: usersController)
                parentVc.present(vc, animated: true)
            }))
        }
        
        if view == .MY_LIBRARY {
            alert.addAction(UIAlertAction(title: "Add to Playlist", style: .default, handler: { _ in
                let playlistsController = SelectPlaylistsController(track)
                playlistsController.modalPresentationStyle = .pageSheet
                
                let vc = UINavigationController(rootViewController: playlistsController)
                parentVc.present(vc, animated: true)
            }))
            alert.addAction(UIAlertAction(title: "Delete", style: .destructive, handler: { _ in
                ViewUtil.showAlert(message: "Delete \(track.name)?", yesText: "Delete", yesStyle: .destructive, dismissText: "Cancel") {
                    TrackService.deleteTrack(track)
                }
            }))
        }
        
        if view == .NOW_PLAYING {
            alert.addAction(UIAlertAction(title: "Remove", style: .default, handler: { _ in
                NowPlayingTracks.removeTracks([track.id])
            }))
        }
        
        if view == .OTHER_USER {
            alert.addAction(UIAlertAction(title: "Import", style: .default, handler: { _ in
                TrackService.importTrack(track)
                Toast.show("Track import started")
            }))
        }
        
        if view == .PLAYLIST {
            alert.addAction(UIAlertAction(title: "Remove from Playlist", style: .default, handler: { _ in
                DispatchQueue.global().async {
                    let playlistTracks = PlaylistTrackDao.findByPlaylistAndTrack(playlistId: playlist!.id, trackId: track.id)
                    if playlistTracks.count > 1 {
                        GGLog.warning("The user is deleting a playlist track that has duplicates. The one they are intending to delete may be different from the one that is deleted")
                    } else if playlistTracks.isEmpty {
                        GGLog.critical("No playlist tracks found when removing a track from a playlist! Track \(track.id). Playlist \(playlist!.id)")
                        return
                    }
                    PlaylistService.removeTrack(playlistTracks.first!)
                }
            }))
        }
        
//        alert.addAction(UIAlertAction(title: "Trim", style: .default, handler: { _ in
//            print("Trim")
//        }))
        
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { _ in
            GGNavLog.info("User clicked context menu 'Cancel' button")
        }))
        
        return alert
    }
}


struct SetPrivateRequest: Codable {
    let trackIds: Array<Int>
    let isPrivate: Bool
}

struct UpdateTrackRequest: Codable {
    let trackIds: Array<Int>
    var name: String? = nil
    var artist: String? = nil
    var featuring: String? = nil
    var album: String? = nil
    var genre: String? = nil
    var trackNumber: Int? = nil
    var note: String? = nil
    var releaseYear: Int? = nil
    var hidden: Bool? = nil
    var `private`: Bool? = nil
    var albumArtUrl: String? = nil
}

struct DeleteTrackRequest: Codable {
    let trackIds: Array<Int>
}

enum TrackContextView {
    case MY_LIBRARY
    case NOW_PLAYING
    case OTHER_USER
    case PLAYLIST
}
