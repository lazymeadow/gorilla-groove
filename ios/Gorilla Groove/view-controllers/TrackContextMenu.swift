import Foundation
import UIKit

class TrackContextMenu {
    static func createMenuForTrack(
        _ track: Track,
        parentVc: UIViewController,
        onAction: @escaping (Track?) -> Void
    ) -> UIAlertController {
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        alert.addAction(UIAlertAction(title: "Edit Properties", style: .default, handler: { _ in
            let metadataController = EditMetadataController(track: track)
            metadataController.modalPresentationStyle = .pageSheet
            
            let vc = UINavigationController(rootViewController: metadataController)
            parentVc.present(vc, animated: true)
        }))
        
//        alert.addAction(UIAlertAction(title: "Recommend", style: .default, handler: { (_) in
//            print("Recommend")
//        }))
        
//        alert.addAction(UIAlertAction(title: "Trim", style: .default, handler: { (_) in
//            print("Trim")
//        }))
        
        alert.addAction(UIAlertAction(title: "Delete", style: .destructive, handler: { (_) in
            ViewUtil.showAlert(message: "Delete \(track.name)?", yesText: "Delete", yesStyle: .destructive) {
                let request = UpdateTrackRequest(trackIds: [track.id])
                HttpRequester.delete("track", request) { _, statusCode, _ in
                    if statusCode.isSuccessful() {
                        onAction(nil)
                        ServerSynchronizer.syncWithServer()
                    } else {
                        Toast.show("Failed to delete the track!")
                    }
                }
            }
        }))
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { (_) in
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
