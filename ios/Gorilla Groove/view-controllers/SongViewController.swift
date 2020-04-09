import UIKit
import Foundation
import AVKit

class SongViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, UISearchResultsUpdating, UISearchBarDelegate {

    var tracks: Array<Track> = []
    var visibleTracks: Array<Track> = []
    
    var contactsTableView = UITableView()
    var searchController = UISearchController()
    var persistentSearchTerm = ""
    var searchWasCanceled = false
    var handlingSearchEnd = false
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let config = UIImage.SymbolConfiguration(pointSize: UIFont.systemFontSize * 1.2, weight: .medium, scale: .large)
        let searchIcon = UIImage(systemName: "magnifyingglass", withConfiguration: config)!
        
        self.navigationItem.rightBarButtonItem = UIBarButtonItem(
              image: searchIcon,
              style: .plain,
              target: self,
              action: #selector(search)
        )
        
        searchController.obscuresBackgroundDuringPresentation = false
        self.navigationItem.hidesSearchBarWhenScrolling = false

        searchController.searchResultsUpdater = self
        searchController.searchBar.delegate = self
        
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
        
        // viewDidLoad only seems to be called once. But I am wary of more than one of these being registered
        NowPlayingTracks.addTrackChangeObserver { _ in
            DispatchQueue.main.async {
                self.contactsTableView.visibleCells.forEach { cell in
                    let songViewCell = cell as! SongViewCell
                    songViewCell.checkIfPlaying()
                }
            }
        }
    }
    
    func updateSearchResults(for searchController: UISearchController) {
        let searchTerm = searchController.searchBar.text!.lowercased()

        if (searchTerm.isEmpty) {
            visibleTracks = tracks
        } else {
            visibleTracks = tracks.filter { $0.name.lowercased().contains(searchTerm) }
        }
        
        contactsTableView.reloadData()
    }

    func searchBarTextDidEndEditing(_ searchBar: UISearchBar) {
        // This is triggered when isActive is set to false. We call this method manually sometimes, and set
        // isActive to false at that time. This will cause a re-trigger that we want to ignore.
        // Unfortunately we can't just listen for "searchController.isActive" because that is set false too late
        if (handlingSearchEnd) {
            return
        }
        
        // When you mark a search bar as inactive the term is unhelpfully cleared out. Put it back
        persistentSearchTerm = searchBar.text!
        
        // We want to ignore the re-trigger that iOS does for ending the event when we mark it as inactive manually.
        // Keep around a boolean so we know to ignore it.
        handlingSearchEnd = true
        searchController.isActive = false
        handlingSearchEnd = false
        
        searchBar.text = persistentSearchTerm
    }
    
    func searchBarCancelButtonClicked(_ searchBar: UISearchBar) {
        searchController.isActive = false
        self.navigationItem.searchController = nil
    }
    
    @objc func search(_ sender: Any) {
        if (self.navigationItem.searchController == nil) {
            self.navigationItem.searchController = searchController
        } else {
            self.navigationItem.searchController = nil
        }
        
        let bar = self.navigationController?.navigationBar
        bar!.sizeToFit()
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
            searchBarTextDidEndEditing(search.searchBar)
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
