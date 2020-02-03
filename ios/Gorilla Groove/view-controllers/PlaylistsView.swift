import Foundation
import UIKit


class PlaylistsView : UITableViewController {

    let trackState = TrackState()
    var playlists: Array<Playlist> = []
    
    override func viewDidLoad() {
        print("Loaded playlists")
        super.viewDidLoad()
        self.title = "Playlists"

        let view = self.view as! UITableView
        
        view.register(UITableViewCell.self, forCellReuseIdentifier: "libraryCell")
        
        // Remove extra table row lines that have no content
        view.tableFooterView = UIView(frame: .zero)
        
        playlists = trackState.getPlaylists()
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return playlists.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "libraryCell", for: indexPath)
        
        cell.textLabel!.text = playlists[indexPath.row].name!
        cell.textLabel!.textColor = #colorLiteral(red: 0.1764705926, green: 0.4980392158, blue: 0.7568627596, alpha: 1)
        
        return cell
    }
}
