import UIKit
import Foundation
import AVFoundation
import AVKit

class AlbumViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    var albums: Array<Album> = []
    var trackState: TrackState? = nil
    let contactsTableView = UITableView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(contactsTableView)
        
        contactsTableView.translatesAutoresizingMaskIntoConstraints = false
        contactsTableView.topAnchor.constraint(equalTo:view.topAnchor).isActive = true
        contactsTableView.leftAnchor.constraint(equalTo:view.leftAnchor).isActive = true
        contactsTableView.rightAnchor.constraint(equalTo:view.rightAnchor).isActive = true
        contactsTableView.bottomAnchor.constraint(equalTo:view.bottomAnchor).isActive = true
        
        contactsTableView.dataSource = self
        contactsTableView.delegate = self
        contactsTableView.register(AlbumViewCell.self, forCellReuseIdentifier: "albumCell")
        
        // Remove extra table rows when we don't have a full screen of songs
        contactsTableView.tableFooterView = UIView(frame: .zero)
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
        
        let tracks = trackState!.getTracks(album: cell.album!.name)
        
        let view = SongViewController(cell.album!.name, tracks)
        self.navigationController!.pushViewController(view, animated: true)
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let albumViewCell = cell as! AlbumViewCell
        
        if (albums[indexPath.row].imageLoadFired) {
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
                    
                    let foundCell = self.contactsTableView.visibleCells.first { rawCell in
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
    init(_ title: String, _ albums: Array<Album>, _ trackState: TrackState) {
        self.albums = albums
        self.trackState = trackState
        
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
}
