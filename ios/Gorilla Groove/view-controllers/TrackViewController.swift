import UIKit
import Foundation
import AVKit

class TrackViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {

    var tracks: Array<Track> = []
    var visibleTracks: Array<Track> = []
    let scrollPlayedTrackIntoView: Bool
    let showingHidden: Bool
    let tableView = UITableView()
       
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(tableView)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.topAnchor.constraint(equalTo:view.topAnchor).isActive = true
        tableView.leftAnchor.constraint(equalTo:view.leftAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo:view.rightAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo:view.bottomAnchor).isActive = true
        
        tableView.keyboardDismissMode = .onDrag
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(TrackViewCell.self, forCellReuseIdentifier: "songCell")
        
        // Remove extra table rows when we don't have a full screen of songs
        tableView.tableFooterView = UIView(frame: .zero)
        
        TableSearchAugmenter.addSearchToNavigation(controller: self, tableView: tableView) { input in
            let searchTerm = input.lowercased()
            if (searchTerm.isEmpty) {
                self.visibleTracks = self.tracks
            } else {
                self.visibleTracks = self.tracks.filter { $0.name.lowercased().contains(searchTerm) }
            }
        }
        
        // viewDidLoad only seems to be called once. But I am wary of more than one of these being registered
        NowPlayingTracks.addTrackChangeObserver { _ in
            DispatchQueue.main.async {
                self.tableView.visibleCells.forEach { cell in
                    let songViewCell = cell as! TrackViewCell
                    songViewCell.checkIfPlaying()
                }
            }
        }
        
        if scrollPlayedTrackIntoView && NowPlayingTracks.nowPlayingIndex >= 0 {
            let indexPath = IndexPath(row: NowPlayingTracks.nowPlayingIndex, section: 0)
            tableView.scrollToRow(at: indexPath, at: .top, animated: false)
        }
    }
 
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleTracks.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "songCell", for: indexPath) as! TrackViewCell
        let track = visibleTracks[indexPath.row]
        
        cell.tableIndex = indexPath.row
        cell.track = track
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        
        let longPressGesture = UILongPressGestureRecognizer(target: self, action: #selector(bringUpSongContextMenu(sender:)))
        cell.addGestureRecognizer(longPressGesture)
            
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let songViewCell = cell as! TrackViewCell
        songViewCell.checkIfPlaying()
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! TrackViewCell
        
        cell.animateSelectionColor()
        NowPlayingTracks.setNowPlayingTracks(visibleTracks, playFromIndex: cell.tableIndex)
        
        if let search = self.navigationItem.searchController {
            search.searchBar.delegate!.searchBarTextDidEndEditing!(search.searchBar)
        }
    }
    
    @objc private func bringUpSongContextMenu(sender: UITapGestureRecognizer) {
        if sender.state != .began {
            return
        }
        
        let cell = sender.view as! TrackViewCell
        let track = visibleTracks[cell.tableIndex]

        let alert = TrackContextMenu.createMenuForTrack(track) { newTrack in
            print(track.isPrivate)
            if newTrack == nil || (!self.showingHidden && newTrack!.isHidden) {
                print("Hiding track")
                self.visibleTracks.remove(at: cell.tableIndex)
                DispatchQueue.main.async {
                    self.tableView.deleteRows(at: [IndexPath(row: cell.tableIndex, section: 0)], with: .automatic)
                }
            }
        }
        
        ViewUtil.showAlert(alert)
    }
    
    init(_ title: String, _ tracks: Array<Track>, scrollPlayedTrackIntoView: Bool = false, showingHidden: Bool = false) {
        self.tracks = tracks
        self.visibleTracks = tracks
        self.scrollPlayedTrackIntoView = scrollPlayedTrackIntoView
        self.showingHidden = showingHidden
        
        super.init(nibName: nil, bundle: nil)

        self.title = title
    }
    
    required init?(coder aDecoder: NSCoder) {
        self.scrollPlayedTrackIntoView = false
        self.showingHidden = false
        super.init(coder: aDecoder)
    }
}

struct TrackLinkResponse: Codable {
    let songLink: String
    let albumArtLink: String
}
