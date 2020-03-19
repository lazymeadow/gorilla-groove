import UIKit
import Foundation
import AVFoundation
import AVKit

class AlbumViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    var albums: Array<Album> = []
    var artist: String? = nil
    var trackState: TrackState? = nil
    let albumTableView = UITableView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(albumTableView)
        
        albumTableView.translatesAutoresizingMaskIntoConstraints = false
        albumTableView.topAnchor.constraint(equalTo:view.topAnchor).isActive = true
        albumTableView.leftAnchor.constraint(equalTo:view.leftAnchor).isActive = true
        albumTableView.rightAnchor.constraint(equalTo:view.rightAnchor).isActive = true
        albumTableView.bottomAnchor.constraint(equalTo:view.bottomAnchor).isActive = true
        
        albumTableView.dataSource = self
        albumTableView.delegate = self
        albumTableView.register(AlbumViewCell.self, forCellReuseIdentifier: "albumCell")
        
        // Remove extra table rows when we don't have a full screen of songs
        albumTableView.tableFooterView = UIView(frame: .zero)
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return albums.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "albumCell", for: indexPath) as! AlbumViewCell
        let album = albums[indexPath.row]
        
        cell.tableIndex = indexPath.row
        cell.album = album
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! AlbumViewCell
        
        cell.animateSelectionColor()
        
        // If someone picked the special "View All" album at the top, then load everything by nilling this out
        let albumToLoad = cell.album!.viewAllAlbum ? nil : cell.album!.name
        let viewName = cell.album!.viewAllAlbum ? "All " + artist! : albumToLoad!
        
        let tracks = trackState!.getTracks(album: albumToLoad, artist: artist)
        
        let view = SongViewController(viewName, tracks)
        self.navigationController!.pushViewController(view, animated: true)
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let albumViewCell = cell as! AlbumViewCell
        
        if (albums[indexPath.row].imageLoadFired || albums[indexPath.row].linkRequestLink.isEmpty) {
            return
        }
        
        albums[indexPath.row].imageLoadFired = true
        
        DispatchQueue.global().async {
            HttpRequester.get(
                self.albums[indexPath.row].linkRequestLink,
                TrackLinkResponse.self
            ) { response, status, err in
                if (status < 200 || status >= 300) {
                    return
                }
                
                let art = UIImage.fromUrl(response!.albumArtLink)
                DispatchQueue.main.async {
                    self.albums[indexPath.row].art = art
                    
                    let foundCell = self.albumTableView.visibleCells.first { rawCell in
                        let cell = rawCell as! AlbumViewCell
                        return cell.tableIndex == indexPath.row
                    }
                    
                    if (foundCell != nil) {
                        albumViewCell.album = self.albums[indexPath.row]
                    }
                }
            }
        }
    }
    
    // It's stupid to pass in trackState. I just need to make it a singleton.
    // When I don't do this, the tracks get garbage collected when you play a song and navigate away from this view
    init(_ title: String, _ albums: Array<Album>, _ artist: String?, _ trackState: TrackState) {
        self.trackState = trackState
        self.artist = artist
        
        var displayedAlbums = albums
        if (artist != nil && displayedAlbums.count > 1) {
            let viewAll = Album(
                name: "View All",
                linkRequestLink: "",
                art: nil,
                imageLoadFired: false,
                viewAllAlbum: true
            )
            
            displayedAlbums.insert(viewAll, at: 0)
        }
        
        self.albums = displayedAlbums
        
        super.init(nibName: nil, bundle: nil)

        self.title = title
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}

struct Album {
    let name: String
    let linkRequestLink: String
    var art: UIImage? = nil
    var imageLoadFired: Bool = false
    var viewAllAlbum: Bool = false
}
