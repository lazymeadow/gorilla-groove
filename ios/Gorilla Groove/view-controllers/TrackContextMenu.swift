import Foundation
import UIKit

class TrackContextMenu {
    static func createMenuForTrack(
        _ track: Track,
        view: TrackContextView,
        parentVc: UIViewController
    ) -> UIAlertController {
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
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
}
