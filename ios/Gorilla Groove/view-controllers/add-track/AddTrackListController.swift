import Foundation
import UIKit

class AddTrackListController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    
    static let title = "Add Songs"

    private var tableItems = TableOption.allCases
    private let tableView = UITableView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(tableView)
        
        self.title = AddTrackListController.title
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        
        tableView.keyboardDismissMode = .onDrag
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(AddTrackListCell.self, forCellReuseIdentifier: "addListCell")
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        GGNavLog.info("Loaded add track list controller")
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tableItems.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "addListCell", for: indexPath) as! AddTrackListCell
        let item = tableItems[indexPath.row]
        
        cell.tableOption = item
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
            
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! AddTrackListCell
        
        cell.animateSelectionColor()
        
        let vc: UIViewController
        
        switch cell.tableOption! {
        case .YOUTUBE_DL:
            vc = DownloadFromYTController()
        case .SPOTIFY_SEARCH:
            vc = AddFromSpotifyController()
        }
        
        vc.modalPresentationStyle = .fullScreen
        self.navigationController!.pushViewController(vc, animated: true)
    }
}

private enum TableOption : CaseIterable {
    case YOUTUBE_DL
    case SPOTIFY_SEARCH
}

fileprivate class AddTrackListCell : UITableViewCell {
    var tableOption: TableOption? {
        didSet {
            guard let option = tableOption else {
                textLabel!.text = ""
                return
            }
            
            switch option {
            case .YOUTUBE_DL:
                textLabel!.text = "Download from YouTube"
            case .SPOTIFY_SEARCH:
                textLabel!.text = "Search Spotify"
            }
        }
    }
}
