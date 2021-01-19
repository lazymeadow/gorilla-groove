import Foundation
import UIKit

class AddTrackListController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    
    static let title = "Add Songs"

    private var tableItems = TableOption.allCases
    private let tableView = UITableView()
    
    private let taskProgressLabel: UILabel = {
        let label = UILabel()
        
        label.textColor = Colors.foreground
        label.font = label.font.withSize(15)
        label.textAlignment = .left
        label.translatesAutoresizingMaskIntoConstraints = false
                
        return label
    }()
    
    private lazy var taskProgressView: UIView = {
//        let rightChevron = IconView("chevron.right", weight: .medium)
//        rightChevron.translatesAutoresizingMaskIntoConstraints = false
//        rightChevron.tintColor = Colors.foreground
        
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = Colors.navigationBackground
                
        view.addSubview(taskProgressLabel)
//        view.addSubview(rightChevron)
        
        NSLayoutConstraint.activate([
//            view.heightAnchor.constraint(equalTo: taskProgressLabel.heightAnchor, constant: 20),
            taskProgressLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            taskProgressLabel.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 15),
//            rightChevron.rightAnchor.constraint(equalTo: view.rightAnchor),
//            rightChevron.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
//
//        view.addGestureRecognizer(UITapGestureRecognizer(
//            target: self,
//            action: #selector(pickSource(tapGestureRecognizer:))
//        ))
        
        return view
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(tableView)
        view.addSubview(taskProgressView)
        
        self.title = AddTrackListController.title
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: taskProgressView.topAnchor),
            taskProgressView.leftAnchor.constraint(equalTo: view.leftAnchor),
            taskProgressView.rightAnchor.constraint(equalTo: view.rightAnchor),
            taskProgressView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            taskProgressView.heightAnchor.constraint(equalToConstant: 0), // For later manipulation
        ])
                
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(AddTrackListCell.self, forCellReuseIdentifier: "addListCell")
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        BackgroundTaskService.registerAddTrackController(self)
        
        recalculateProgressText()
    }
    
    func recalculateProgressText() {
        if !Thread.isMainThread {
            DispatchQueue.main.async {
                self.recalculateProgressText()
            }
            return
        }
        
        let heightConstraint = self.taskProgressView.constraints.filter({ $0.firstAttribute == .height }).first!
        
        let tasks = BackgroundTaskService.tasks
        if tasks.isEmpty {
            taskProgressLabel.text = ""
            heightConstraint.constant = 0
        } else {
            heightConstraint.constant = 30
            
            let finishedTasks = tasks.filter { $0.status == .COMPLETE || $0.status == .FAILED }

            taskProgressLabel.text = "Processing \(finishedTasks.count + 1) of \(tasks.count) downloads..."
        }
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
