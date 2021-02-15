import Foundation
import UIKit

class TrackContextMenu {
    static func createMenuForTracks(
        _ tracks: [Track],
        view: TrackContextView,
        playlist: Playlist?,
        parentVc: UIViewController
    ) -> UIAlertController {
        let alert = GGActionSheet.create()
        
        alert.addAction(UIAlertAction(title: "Play Next", style: .default, handler: { _ in
            NowPlayingTracks.addTracksNext(tracks)
        }))
        alert.addAction(UIAlertAction(title: "Play Last", style: .default, handler: { _ in
            NowPlayingTracks.addTracksLast(tracks)
        }))
        
        if view == .MY_LIBRARY || view == .NOW_PLAYING {
            // Not all menus are usable with multiple tracks selected
            if tracks.count == 1 {
                alert.addAction(UIAlertAction(title: "Edit Properties", style: .default, handler: { _ in
                    let metadataController = EditMetadataController(track: tracks.first!)
                    metadataController.modalPresentationStyle = .pageSheet
                    
                    let vc = UINavigationController(rootViewController: metadataController)
                    parentVc.present(vc, animated: true)
                }))
                
                alert.addAction(UIAlertAction(title: "Trim", style: .default, handler: { _ in
                    let trackTrimController = TrimTrackController(tracks.first!)
                    trackTrimController.modalPresentationStyle = .pageSheet
                    
                    let vc = UINavigationController(rootViewController: trackTrimController)
                    parentVc.present(vc, animated: true)
                }))
            }
            
            alert.addAction(UIAlertAction(title: "Recommend", style: .default, handler: { _ in
                let usersController = SelectUsersController(tracks)
                usersController.modalPresentationStyle = .pageSheet
                
                let vc = UINavigationController(rootViewController: usersController)
                parentVc.present(vc, animated: true)
            }))
            
            alert.addAction(UIAlertAction(title: "Add to Playlist", style: .default, handler: { _ in
                let playlistsController = SelectPlaylistsController(tracks)
                playlistsController.modalPresentationStyle = .pageSheet
                
                let vc = UINavigationController(rootViewController: playlistsController)
                parentVc.present(vc, animated: true)
            }))
        }
        
        if view == .MY_LIBRARY {
            alert.addAction(UIAlertAction(title: "Delete", style: .destructive, handler: { _ in
                let message = tracks.count == 1 ? tracks.first!.name : "the selected \(tracks.count) tracks"
                ViewUtil.showAlert(message: "Delete \(message)?", yesText: "Delete", yesStyle: .destructive, dismissText: "Cancel") {
                    TrackService.deleteTracks(tracks)
                }
            }))
        }
        
        if view == .NOW_PLAYING {
            alert.addAction(UIAlertAction(title: "Remove", style: .default, handler: { _ in
                NowPlayingTracks.removeTracks(Set(tracks.map { $0.id }))
            }))
        }
        
        if view == .OTHER_USER {
            alert.addAction(UIAlertAction(title: "Import", style: .default, handler: { _ in
                TrackService.importTracks(tracks)
                Toast.show("Track import started")
            }))
        }
        
        if view == .PLAYLIST {
            alert.addAction(UIAlertAction(title: "Remove from Playlist", style: .destructive, handler: { _ in
                DispatchQueue.global().async {
                    
                }
            }))
        }
        
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
    let trackIds: [Int]
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
