import Foundation
import UIKit

class TrackContextMenu {
    static func createMenuForTrack(_ track: Track) -> UIAlertController {
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        let privateTitle = track.isPrivate ? "Make Public" : "Make Private"
        alert.addAction(UIAlertAction(title: privateTitle, style: .default, handler: { (_) in
            track.isPrivate = !track.isPrivate
            let request = SetPrivateRequest(trackIds: [track.id], isPrivate: track.isPrivate)
            HttpRequester.post("track/set-private", EmptyResponse.self, request) { _, statusCode, _ in
                if statusCode.isSuccessful() {
                    ServerSynchronizer.syncWithServer()
                } else {
                    ViewUtil.showAlert(message: "Failed to update track visibility!", async: true)
                }
            }
        }))
        
        alert.addAction(UIAlertAction(title: "Recommend", style: .default, handler: { (_) in
            print("Recommend")
        }))
        
        alert.addAction(UIAlertAction(title: "Trim", style: .default, handler: { (_) in
            print("Trim")
        }))
        
        alert.addAction(UIAlertAction(title: "Hide from Library", style: .default, handler: { (_) in
            print("User clicked hide")
        }))
        
        alert.addAction(UIAlertAction(title: "View Info", style: .default, handler: { (_) in
            print("User clicked view info")
        }))
        
        alert.addAction(UIAlertAction(title: "Delete", style: .destructive, handler: { (_) in
            print("User click Delete button")
        }))
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { (_) in
            print("User click Dismiss button")
        }))
        
        return alert
    }
}
