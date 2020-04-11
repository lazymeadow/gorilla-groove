import UIKit
import Foundation
import AVKit

class SongViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {

    var tracks: Array<Track> = []
    var visibleTracks: Array<Track> = []
       
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let contactsTableView = UITableView()

        view.addSubview(contactsTableView)
        
        contactsTableView.translatesAutoresizingMaskIntoConstraints = false
        contactsTableView.topAnchor.constraint(equalTo:view.topAnchor).isActive = true
        contactsTableView.leftAnchor.constraint(equalTo:view.leftAnchor).isActive = true
        contactsTableView.rightAnchor.constraint(equalTo:view.rightAnchor).isActive = true
        contactsTableView.bottomAnchor.constraint(equalTo:view.bottomAnchor).isActive = true
        
        contactsTableView.keyboardDismissMode = .onDrag
        
        contactsTableView.dataSource = self
        contactsTableView.delegate = self
        contactsTableView.register(SongViewCell.self, forCellReuseIdentifier: "songCell")
        
        // Remove extra table rows when we don't have a full screen of songs
        contactsTableView.tableFooterView = UIView(frame: .zero)
        
        TableSearchAugmenter.addSearchToNavigation(controller: self, tableView: contactsTableView) { input in
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
                contactsTableView.visibleCells.forEach { cell in
                    let songViewCell = cell as! SongViewCell
                    songViewCell.checkIfPlaying()
                }
            }
        }
    }
 
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleTracks.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "songCell", for: indexPath) as! SongViewCell
        let track = visibleTracks[indexPath.row]
        
        cell.tableIndex = indexPath.row
        cell.track = track
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        	
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let songViewCell = cell as! SongViewCell
        songViewCell.checkIfPlaying()
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! SongViewCell
        
        cell.animateSelectionColor()
        NowPlayingTracks.setNowPlayingTracks(visibleTracks, playFromIndex: cell.tableIndex)
        
        if let search = self.navigationItem.searchController {
            search.searchBar.delegate!.searchBarTextDidEndEditing!(search.searchBar)
        }
    }
    
    init(_ title: String, _ tracks: Array<Track>) {
        self.tracks = tracks
        self.visibleTracks = tracks
        
        super.init(nibName: nil, bundle: nil)

        self.title = title
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}

struct TrackLinkResponse: Codable {
    let songLink: String
    let albumArtLink: String
}
