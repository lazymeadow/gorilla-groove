import UIKit
import Foundation
import AVFoundation
import AVKit

class SongViewController: UIViewController, UITableViewDataSource {

    let trackState = TrackState()
    var tracks: Array<Track> = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
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
        
        tracks = trackState.getTracks()
        print(tracks)
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tracks.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "songCell", for: indexPath) as! SongViewCell
        let track = tracks[indexPath.row]
        
        cell.track = track
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! SongViewCell
        
        cell.animateSelectionColor()

        HttpRequester.get("file/link/\(cell.track!.id)?audioFormat=MP3", TrackLinkResponse.self) { links, status , err in
            if (status < 200 || status >= 300 || links == nil) {
                print("Failed to get track links!")
                return
            }
            
            let playerItem = AVPlayerItem(url: URL(string: links!.songLink)!)
            
            AudioPlayer.player.replaceCurrentItem(with: playerItem)
            AudioPlayer.player.playImmediately(atRate: 1.0)
        }
    }
}

struct TrackLinkResponse: Codable {
    let songLink: String
    let albumArtLink: String
}
