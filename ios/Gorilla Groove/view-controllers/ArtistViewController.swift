import UIKit
import Foundation
import AVFoundation
import AVKit

class ArtistViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    var artists: Array<String> = []
    var visibleArtists: Array<String> = []
    
    var trackState: TrackState? = nil
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let artistTableView = UITableView()

        view.addSubview(artistTableView)
        
        artistTableView.translatesAutoresizingMaskIntoConstraints = false
        artistTableView.topAnchor.constraint(equalTo:view.topAnchor).isActive = true
        artistTableView.leftAnchor.constraint(equalTo:view.leftAnchor).isActive = true
        artistTableView.rightAnchor.constraint(equalTo:view.rightAnchor).isActive = true
        artistTableView.bottomAnchor.constraint(equalTo:view.bottomAnchor).isActive = true
        
        artistTableView.dataSource = self
        artistTableView.delegate = self
        artistTableView.register(ArtistViewCell.self, forCellReuseIdentifier: "artistCell")
        
        TableSearchAugmenter.addSearchToNavigation(controller: self, tableView: artistTableView) { input in
            let searchTerm = input.lowercased()
            print(searchTerm)
            if (searchTerm.isEmpty) {
                self.visibleArtists = self.artists
            } else {
                self.visibleArtists = self.artists.filter { $0.lowercased().contains(searchTerm) }
            }
        }
        
        // Remove extra table rows when we don't have a full screen of songs
        artistTableView.tableFooterView = UIView(frame: .zero)
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleArtists.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "artistCell", for: indexPath) as! ArtistViewCell
        let artist = visibleArtists[indexPath.row]
        
        cell.artist = artist
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! ArtistViewCell
        
        cell.animateSelectionColor()
        
        let albums = trackState!.getAlbums(artist: cell.artist!)
        
        // If we only have one album to view, may as well just load it and save ourselves a tap
        let view: UIViewController = {
            if (albums.count == 1) {
                let tracks = self.trackState!.getTracks(album: albums.first!.name, artist: cell.artist!)
                
                return SongViewController(albums.first!.name, tracks)
            } else {
                return AlbumViewController(cell.artist!, albums, cell.artist!, self.trackState!)
            }
        }()
        
        self.navigationController!.pushViewController(view, animated: true)
    }
    
    // It's stupid to pass in trackState. I just need to make it a singleton.
    // When I don't do this, the tracks get garbage collected when you play a song and navigate away from this view
    init(_ title: String, _ artists: Array<String>, _ trackState: TrackState) {
        self.artists = artists
        self.visibleArtists = artists
        self.trackState = trackState
        
        super.init(nibName: nil, bundle: nil)

        self.title = title
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
