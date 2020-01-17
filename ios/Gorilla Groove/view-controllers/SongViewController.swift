import UIKit
import Foundation
import AVFoundation
import AVKit

class SongViewController: UIViewController, UITableViewDataSource {

    let trackState = TrackState()
    var tracks: Array<Track> = []
    var player = AVPlayer(playerItem: nil)
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        try! AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try! AVAudioSession.sharedInstance().setActive(true)
        
        print("Loaded song view controller")
        
        self.player.automaticallyWaitsToMinimizeStalling = false
        self.player.volume = 1.0

        view.backgroundColor = .red
        
        let contactsTableView = UITableView() // view
        view.addSubview(contactsTableView)
        
        let av = AVPlayerViewController()
        av.player = player
        av.view.frame = CGRect(x:-2, y:620, width:380, height:50)
        self.addChild(av)
        self.view.addSubview(av.view)
        av.didMove(toParent: self)
        
        contactsTableView.translatesAutoresizingMaskIntoConstraints = false
        contactsTableView.topAnchor.constraint(equalTo:view.topAnchor).isActive = true
        contactsTableView.leftAnchor.constraint(equalTo:view.leftAnchor).isActive = true
        contactsTableView.rightAnchor.constraint(equalTo:view.rightAnchor).isActive = true
        contactsTableView.bottomAnchor.constraint(equalTo:view.bottomAnchor).isActive = true
        
        contactsTableView.dataSource = self
        contactsTableView.register(SongViewCell.self, forCellReuseIdentifier: "songCell")
        
        tracks = trackState.getTracks()
        print(tracks)
        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
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
            
            self.player.replaceCurrentItem(with: playerItem)
            self.player.playImmediately(atRate: 1.0)
            print("Playing")
        }
    }
}

struct TrackLinkResponse: Codable {
    let songLink: String
    let albumArtLink: String
}
