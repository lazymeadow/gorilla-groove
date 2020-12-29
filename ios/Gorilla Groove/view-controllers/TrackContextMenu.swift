import Foundation
import UIKit

class TrackContextMenu {
    static func createMenuForTrack(_ track: Track, onAction: @escaping (Track?) -> Void) -> UIAlertController {
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        let privateTitle = track.isPrivate ? "Make Public" : "Make Private"
        alert.addAction(UIAlertAction(title: privateTitle, style: .default, handler: { (_) in
            track.isPrivate = !track.isPrivate
            let request = SetPrivateRequest(trackIds: [track.id], isPrivate: track.isPrivate)
            HttpRequester.post("track/set-private", EmptyResponse.self, request) { _, statusCode, _ in
                if statusCode.isSuccessful() {
                    onAction(track)
                    ServerSynchronizer.syncWithServer()
                } else {
                    ViewUtil.showAlert(message: "Failed to update track visibility!")
                }
            }
        }))
        
//        alert.addAction(UIAlertAction(title: "Recommend", style: .default, handler: { (_) in
//            print("Recommend")
//        }))
        
//        alert.addAction(UIAlertAction(title: "Trim", style: .default, handler: { (_) in
//            print("Trim")
//        }))
        
        let hideTitle = track.isHidden ? "Show in Library" : "Hide in Library"
        alert.addAction(UIAlertAction(title: hideTitle, style: .default, handler: { (_) in
            track.isHidden = !track.isHidden
            let request = UpdateTrackRequest(trackIds: [track.id], hidden: track.isHidden)
            HttpRequester.put("track/simple-update", EmptyResponse.self, request) { _, statusCode, _ in
                if statusCode.isSuccessful() {
                    onAction(track)
                    ServerSynchronizer.syncWithServer()
                } else {
                    ViewUtil.showAlert(message: "Failed to update track visibility!")
                }
            }
        }))
        
//        alert.addAction(UIAlertAction(title: "View Info", style: .default, handler: { (_) in
//            print("User clicked view info")
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
    var hidden: Bool? = nil
}

struct DeleteTrackRequest: Codable {
    let trackIds: Array<Int>
}
