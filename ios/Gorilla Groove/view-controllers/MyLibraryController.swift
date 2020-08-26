import UIKit
import AVFoundation
import CoreData

class MyLibraryController: UITableViewController {
    let options = SongViewType.allCases
    
    override func viewDidLoad() {
        print("Loaded my library")

        super.viewDidLoad()

        self.title = "My Library"
        
        try! AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try! AVAudioSession.sharedInstance().setActive(true)
        
        let view = self.view as! UITableView
        
        view.register(UITableViewCell.self, forCellReuseIdentifier: "libraryCell")
        
        // Remove extra table row lines that have no content
        view.tableFooterView = UIView(frame: .zero)
        
        self.navigationItem.leftBarButtonItem = UIBarButtonItem(
            title: "Logout",
            style: .plain,
            target: self,
            action: #selector(logout)
        )
    }
    
    @objc func logout(_ sender: Any) {
        // Call stop() before we actually clear out login state as this updates the server with our device
        AudioPlayer.stop()

        // TODO actually send the logout command to the API
        FileState.clear(LoginState.self)
        
        // Until we ditch the storyboard, have to navigate to the login view this way
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "LoginController")
        
        // When I used the present() code, it would make the Sync view not load properly
        // after someone logged out? It seems like setting the root view is the only way
        // to navigate anywhere (outside a navigation controller) that doesn't have side effects...
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        appDelegate.window!.rootViewController = vc
//        vc.modalPresentationStyle = .fullScreen
//        vc.modalTransitionStyle = .crossDissolve
//        self.present(vc, animated: true)
        
        Database.close()
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return options.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "libraryCell", for: indexPath)
        
        cell.textLabel!.text = "\(options[indexPath.row])".toTitleCase()
        cell.textLabel!.textColor = UIColor(named: "Table Text")
        let tapGesture = UITapGestureRecognizer(
            target: self,
            action: #selector(handleTap(sender:))
        )
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! UITableViewCell
        cell.animateSelectionColor()
        
        let tapLocation = sender.location(in: self.tableView)
        
        let optionIndex = self.tableView.indexPathForRow(at: tapLocation)![1]
        let viewType = options[optionIndex]
        let viewText = "\(viewType)".toTitleCase()
        
        switch (viewType) {
        case .TITLE:
            loadTitleView(viewText)
        case .ARTIST:
            loadArtistView()
        case .ALBUM:
            loadAlbumView()
        case .PLAY_COUNT:
            loadTitleView(viewText, "play_count", sortAscending: false)
        case .DATE_ADDED:
            loadTitleView(viewText, "created_at", sortAscending: false)
        }
    }
    
    @objc private func loadTitleView(
        _ viewTitle: String,
        _ sortOverrideKey: String? = nil,
        sortAscending: Bool = true
    ) {
        let tracks = TrackService.getTracks(sortOverrideKey: sortOverrideKey, sortAscending: sortAscending)

        let view = TrackViewController(viewTitle, tracks)
        self.navigationController!.pushViewController(view, animated: true)
    }
    
    @objc private func loadArtistView() {
        let ownId = FileState.read(LoginState.self)!.id

        let artists = TrackDao.getArtists(userId: ownId)

        let view = ArtistViewController("Artist", artists)
        self.navigationController!.pushViewController(view, animated: true)
    }
    
    @objc private func loadAlbumView() {
        let ownId = FileState.read(LoginState.self)!.id

        let albums = TrackDao.getAlbums(userId: ownId)

        let view = AlbumViewController("Album", albums, nil)
        self.navigationController!.pushViewController(view, animated: true)
    }
    
    enum SongViewType: CaseIterable {
        case TITLE
        case ARTIST
        case ALBUM
        case PLAY_COUNT
        case DATE_ADDED
    }
}

extension String {
    func toTitleCase() -> String {
        return self.replacingOccurrences(of: "_", with: " ").capitalized
    }
}
