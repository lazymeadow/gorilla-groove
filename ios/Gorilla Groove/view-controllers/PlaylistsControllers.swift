import Foundation
import UIKit


class PlaylistsController : UITableViewController {

    var playlists: Array<Playlist> = []
    
    @SettingsBundleStorage(key: "offline_mode_enabled")
    private var offlineModeEnabled: Bool
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Playlists"

        let view = self.view as! UITableView
        
        view.register(TableViewCell<Playlist>.self, forCellReuseIdentifier: "playlistCell")
        
        // Remove extra table row lines that have no content
        view.tableFooterView = UIView(frame: .zero)
        
        let ownId = FileState.read(LoginState.self)!.id

        playlists = PlaylistDao.getPlaylists(userId: ownId)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        GGNavLog.info("Loaded playlists view")
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return playlists.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let rawCell = tableView.dequeueReusableCell(withIdentifier: "playlistCell", for: indexPath)
        let cell = rawCell as! TableViewCell<Playlist>
        
        cell.textLabel!.text = playlists[indexPath.row].name
        cell.data = playlists[indexPath.row]
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! TableViewCell<Playlist>
        cell.animateSelectionColor()
        
        let playlist = cell.data!
        
        let isSongCached = offlineModeEnabled ? true : nil
        let tracks = TrackDao.getTracksForPlaylist(playlist.id, isSongCached: isSongCached)
        
        let view = TrackViewController(playlist.name, tracks, showingHidden: true)
        self.navigationController!.pushViewController(view, animated: true)
    }
}
