import Foundation
import UIKit

class PickReviewSourceController : UIViewController, UITableViewDataSource, UITableViewDelegate {
    private let tableView = UITableView()
    
    private let reviewSources: Array<ReviewSource>
    private var activeSourceId: Int
    private let reviewQueueController: ReviewQueueController
    private let tracksForSource: [Int: [Track]]
    
    init(
        activeSourceId: Int,
        reviewSources: Array<ReviewSource>,
        reviewQueueController: ReviewQueueController,
        tracksForSource: [Int: [Track]]
    ) {
        self.reviewSources = reviewSources.sorted { $0.displayName < $1.displayName }
        self.activeSourceId = activeSourceId
        self.reviewQueueController = reviewQueueController
        self.tracksForSource = tracksForSource
        
        super.init(nibName: nil, bundle: nil)
    }
 
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(tableView)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        tableView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(PickReviewSourceCell.self, forCellReuseIdentifier: "reviewSourceCell")
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return reviewSources.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "reviewSourceCell", for: indexPath) as! PickReviewSourceCell
        let reviewSource = reviewSources[indexPath.row]
        
        cell.reviewSource = reviewSource
        cell.count = tracksForSource[reviewSource.id]!.count
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
            
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let sourceCell = cell as! PickReviewSourceCell
        sourceCell.checkIfSelected(activeSourceId: activeSourceId)
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! PickReviewSourceCell
        let newSourceId = cell.reviewSource!.id
        
        cell.animateSelectionColor()
        
        tableView.visibleCells.forEach { cell in
            (cell as! PickReviewSourceCell).checkIfSelected(activeSourceId: newSourceId)
        }
        
        reviewQueueController.setActiveSource(newSourceId)
        
        navigationController!.popViewController(animated: true)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}



fileprivate class PickReviewSourceCell: UITableViewCell {
    var reviewSource: ReviewSource? {
        didSet {
            guard let reviewSource = reviewSource else { return }
            nameLabel.text = reviewSource.displayName
            nameLabel.sizeToFit()
        }
    }
    
    var count: Int = 0 {
        didSet {
            countBadgeLabel.text = String(count)
            if count > 99 {
                countBadgeLabel.text = "99"
            }
        }
    }
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 18)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let isCheckedImage: UIView = {
        let icon = IconView("checkmark", weight: .medium, scale: .large)
        icon.translatesAutoresizingMaskIntoConstraints = false
        icon.tintColor = Colors.primary
        return icon
    }()
        
    private let countBadgeLabel: UILabel = {
        let label = UILabel()
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        
        label.sizeToFit()
        label.textColor = Colors.foreground
        
        return label
    }()
    
    private lazy var countBadgeContainer: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(countBadgeLabel)
        
        countBadgeLabel.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
        countBadgeLabel.heightAnchor.constraint(equalTo: view.heightAnchor).isActive = true

        view.backgroundColor = Colors.primary
        view.layer.cornerRadius = PickReviewSourceCell.badgeSize / 2
        
        return view
    }()
    
    private static let badgeSize: CGFloat = 28
    
    func checkIfSelected(activeSourceId: Int) {
        isCheckedImage.isHidden = activeSourceId != reviewSource!.id
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.contentView.addSubview(nameLabel)
        self.contentView.addSubview(isCheckedImage)
        self.contentView.addSubview(countBadgeContainer)
        
        NSLayoutConstraint.activate([
            self.contentView.heightAnchor.constraint(equalTo: nameLabel.heightAnchor, constant: 22),
            
            isCheckedImage.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor),
            isCheckedImage.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
            
            nameLabel.trailingAnchor.constraint(equalTo: countBadgeContainer.leadingAnchor, constant: 16),
            nameLabel.leadingAnchor.constraint(equalTo: isCheckedImage.trailingAnchor),
            nameLabel.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
            
            countBadgeContainer.widthAnchor.constraint(equalToConstant: PickReviewSourceCell.badgeSize),
            countBadgeContainer.heightAnchor.constraint(equalToConstant: PickReviewSourceCell.badgeSize),
            countBadgeContainer.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -10),
            countBadgeContainer.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
