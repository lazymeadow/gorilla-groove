import Foundation
import UIKit

class EditReviewSourcesController : UIViewController, UITableViewDataSource, UITableViewDelegate {
    private let tableView = UITableView()
    
    private var reviewSourcesByType: [SourceType: [ReviewSource]]
    private var activeSourceId: Int?
    private let reviewQueueController: ReviewQueueController
    
    private static let queueTypesToShow = Set([SourceType.YOUTUBE_CHANNEL, .ARTIST])
    
    init(
        activeSourceId: Int?,
        reviewSources: Array<ReviewSource>,
        reviewQueueController: ReviewQueueController
    ) {
        self.reviewSourcesByType = reviewSources
            .filter { EditReviewSourcesController.queueTypesToShow.contains($0.sourceType) }
            .sorted { $0.displayName.localizedCompare($1.displayName) == .orderedAscending }
            .groupBy { $0.sourceType }
        
        self.activeSourceId = activeSourceId
        self.reviewQueueController = reviewQueueController
        
        super.init(nibName: nil, bundle: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Manage Queues"
        
        self.navigationItem.rightBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .add,
            target: self,
            action: #selector(addReviewSource)
        )
        
        view.addSubview(tableView)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(EditReviewSourceCell.self, forCellReuseIdentifier: "editReviewSourceCell")
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    @objc func addReviewSource() {
        GGNavLog.info("User tapped add review source")
        
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: "Artist", style: .default, handler: { (_) in
            GGNavLog.info("User tapped add Artist source")
            
            let vc = AddReviewSourcesController(sourceType: .ARTIST, editReviewController: self)
            vc.modalPresentationStyle = .fullScreen
            
            self.navigationController!.pushViewController(vc, animated: true)
        }))
        alert.addAction(UIAlertAction(title: "YouTube Channel", style: .default, handler: { (_) in
            GGNavLog.info("User tapped add YouTube Channel source")

            let vc = AddReviewSourcesController(sourceType: .YOUTUBE_CHANNEL, editReviewController: self)
            vc.modalPresentationStyle = .fullScreen
            
            self.navigationController!.pushViewController(vc, animated: true)
        }))
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { (_) in
            GGNavLog.info("User tapped cancel button")
        }))
//        let vc = EditReviewSourcesController(
//            reviewQueueController: self
//        )
//        vc.modalPresentationStyle = .fullScreen
//
//        self.navigationController!.pushViewController(vc, animated: true)
        
        ViewUtil.showAlert(alert)
    }
    
    func addSource(_ source: ReviewSource) {
        if reviewSourcesByType[source.sourceType] == nil {
            reviewSourcesByType[source.sourceType] = []
        }
        
        reviewSourcesByType[source.sourceType]!.append(source)
        reviewSourcesByType[source.sourceType]!.sort { $0.displayName.localizedCompare($1.displayName) == .orderedAscending }
        
        tableView.reloadData()
        
        reviewQueueController.onSourcesSynced()
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if section == 0 {
            return "Artists"
        } else if section == 1 {
            return "YouTube Channels"
        } else {
            fatalError("Unknown section encountered: \(section)!")
        }
    }
    
    private func sectionToSourceType(_ section: Int) -> SourceType {
        if section == 0 {
            return .ARTIST
        } else if section == 1 {
            return .YOUTUBE_CHANNEL
        } else {
            fatalError("Unknown section encountered: \(section)!")
        }
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return EditReviewSourcesController.queueTypesToShow.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return reviewSourcesByType[sectionToSourceType(section)]?.count ?? 0
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "editReviewSourceCell", for: indexPath) as! EditReviewSourceCell
        let reviewSource = reviewSourcesByType[sectionToSourceType(indexPath.section)]![indexPath.row]
        
        cell.reviewSource = reviewSource
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
            
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! EditReviewSourceCell
//        let newSourceId = cell.reviewSource!.id
        
        cell.animateSelectionColor()
        
//        tableView.visibleCells.forEach { cell in
//            (cell as! EditReviewSourceCell).checkIfSelected(activeSourceId: newSourceId)
//        }
        
//        reviewQueueController.setActiveSource(newSourceId)
        
//        navigationController!.popViewController(animated: true)
    }
    
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class EditReviewSourceCell: UITableViewCell {
    var reviewSource: ReviewSource? {
        didSet {
            guard let reviewSource = reviewSource else { return }
            nameLabel.text = reviewSource.displayName
            nameLabel.sizeToFit()
        }
    }
    
    private let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 18)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.contentView.addSubview(nameLabel)
        
        NSLayoutConstraint.activate([
            self.contentView.heightAnchor.constraint(equalTo: nameLabel.heightAnchor, constant: 22),
            
            nameLabel.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -10),
            nameLabel.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 10),
            nameLabel.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

