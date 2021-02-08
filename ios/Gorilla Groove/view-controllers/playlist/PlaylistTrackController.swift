import Foundation
import UIKit

class PlaylistTrackController : TrackViewController {
    
    private var isOrderingEdited = false
    
    override func viewDidLoad() {
        if originalView == .PLAYLIST {
            self.navigationItem.rightBarButtonItem = UIBarButtonItem(
                barButtonSystemItem: .edit,
                target: self,
                action: #selector(toggleEditMode)
            )
        }
        
        super.viewDidLoad()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        setEditing(false, animated: false)
        
        super.viewWillDisappear(animated)
    }
    
    func tableView(_ tableView: UITableView, moveRowAt sourceIndexPath: IndexPath, to destinationIndexPath: IndexPath) {
        let movedTrackId = visibleTrackIds[sourceIndexPath.row]
        visibleTrackIds.remove(at: sourceIndexPath.row)
        visibleTrackIds.insert(movedTrackId, at: destinationIndexPath.row)

        isOrderingEdited = true
    }
    
    // These two functions remove the "delete" functionality of edit mode
    func tableView(_ tableView: UITableView, editingStyleForRowAt indexPath: IndexPath) -> UITableViewCell.EditingStyle {
        return .none
    }
    
    func tableView(_ tableView: UITableView, shouldIndentWhileEditingRowAt indexPath: IndexPath) -> Bool {
        return false
    }
    
    override func editingChanged(_ isEditing: Bool) {
        if isEditing {
            cancelSearch()
        } else if isOrderingEdited {
            isOrderingEdited = false
            
            persistPlaylistOrder()
        }
    }
    
    @objc private func toggleEditMode() {
        setEditing(!tableView.isEditing, animated: true)
    }
    
    private func persistPlaylistOrder() {
        if visibleTrackIds.count != trackIds.count {
            Toast.show("Could not persist playlist order")
            GGLog.critical("Not all tracks were visible when persisting playlist order")
            
            return
        }
        
        GGLog.info("Updating sort order for tracks... ")

        var playlistTrackWithIndex: [(PlaylistTrackWithTrack, Int)] = []
        visibleTrackIds.indices.forEach { i in
            let trackId = visibleTrackIds[i]
            let playlistTrack = trackIdToTrack[trackId] as! PlaylistTrackWithTrack
            
            playlistTrackWithIndex.append((playlistTrack, i))
        }
        
        DispatchQueue.global().async { [weak self] in
            guard let this = self else { return }

            let request = ReorderPlaylistRequest(playlistId: this.playlist!.id, playlistTrackIds: playlistTrackWithIndex.map { $0.0.id })
            HttpRequester.put("playlist/track/sort-order", EmptyResponse.self, request) { _, status, _ in
                if !status.isSuccessful() {
                    Toast.show("Could not upload new sort order. It will be saved on-device.")
                } else {
                    GGLog.info("Successfully updated sort order on the API")
                }
            }
            
            // There isn't really a good reason to not update the order locally even if it gets out of sync with the server.
            // So launch the actions independently.
            playlistTrackWithIndex.forEach { (playlistTrack, index) in
                PlaylistTrackDao.setSortOrderForPlaylistTrackId(playlistTrack.id, sortOrder: index)
            }
        }
    }
}

struct ReorderPlaylistRequest : Codable {
    let playlistId: Int
    let playlistTrackIds: [Int]
}
