import UIKit
import Foundation
import AVFoundation
import AVKit

class SongViewController: UIViewController, UITableViewDataSource {

    let trackState = TrackState()
    var tracks: Array<Track> = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Title"
        
        try! AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try! AVAudioSession.sharedInstance().setActive(true)
        
        print("Loaded song view controller")

        view.backgroundColor = .red
        
        let contactsTableView = UITableView() // view
        view.addSubview(contactsTableView)
        
        contactsTableView.translatesAutoresizingMaskIntoConstraints = false
        contactsTableView.topAnchor.constraint(equalTo:view.topAnchor).isActive = true
        contactsTableView.leftAnchor.constraint(equalTo:view.leftAnchor).isActive = true
        contactsTableView.rightAnchor.constraint(equalTo:view.rightAnchor).isActive = true
        contactsTableView.bottomAnchor.constraint(equalTo:view.bottomAnchor).isActive = true
        
        contactsTableView.dataSource = self
        contactsTableView.register(SongViewCell.self, forCellReuseIdentifier: "songCell")
        
        // Remove extra table rows when we don't have a full screen of songs
        contactsTableView.tableFooterView = UIView(frame: .zero)
        
        tracks = trackState.getTracks()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tracks.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "songCell", for: indexPath) as! SongViewCell
        let track = tracks[indexPath.row]
        
        cell.tableIndex = indexPath.row
        cell.track = track
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! SongViewCell
        
        cell.animateSelectionColor()
        NowPlayingTracks.setNowPlayingTracks(self.tracks, playFromIndex: cell.tableIndex)
    }
}

struct TrackLinkResponse: Codable {
    let songLink: String
    let albumArtLink: String
}
