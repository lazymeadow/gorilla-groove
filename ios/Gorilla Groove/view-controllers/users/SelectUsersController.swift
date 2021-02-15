import Foundation
import UIKit

class SelectUsersController : BaseUsersController<SelectUserCell> {
    
    private var selectedUserIds = Set<Int>()
    private let tracks: [Track]
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.color = Colors.foreground
        spinner.hidesWhenStopped = true
        
        return spinner
    }()
    
    private lazy var sendButton = UIBarButtonItem(title: "Send", style: .plain, action: { [weak self] in self?.sendRecommendations() })

    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.view.addSubview(activitySpinner)
        
        NSLayoutConstraint.activate([
            activitySpinner.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: -23),
            activitySpinner.topAnchor.constraint(equalTo: self.view.topAnchor, constant: 10),
        ])
        
        var newNavItems = self.navigationItem.rightBarButtonItems ?? []
        newNavItems.insert(sendButton, at: 0)
        self.navigationItem.rightBarButtonItems = newNavItems
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        GGNavLog.info("Loaded select users controller")
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = super.tableView(tableView, cellForRowAt: indexPath) as! SelectUserCell
        
        cell.isActive = selectedUserIds.contains(cell.user!.id)
        
        return cell
    }
    
    override func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! SelectUserCell
        
        cell.animateSelectionColor()
        
        let user = cell.user!
        
        if cell.isActive {
            selectedUserIds.remove(user.id)
        } else {
            selectedUserIds.insert(user.id)
        }
        
        cell.isActive = !cell.isActive
    }
    
    func sendRecommendations() {
        // Prevent double taps
        if activitySpinner.isAnimating { return }
        
        if selectedUserIds.isEmpty { return }
        
        activitySpinner.startAnimating()
        // Hide the button by changing the text color. If we just remove it, then the filter option will shift weirdly
        sendButton.tintColor = Colors.navigationBackground
        
        let request = ReviewQueueRecommendRequest(trackIds: tracks.map { $0.id }, targetUserIds: selectedUserIds.toArray())
        let this = self
        HttpRequester.post("review-queue/recommend", EmptyResponse.self, request) { _, status, _ in
            if !status.isSuccessful() {
                DispatchQueue.main.async {
                    Toast.show("Failed to recommend track", view: this.view)
                    this.sendButton.tintColor = Colors.primary
                    this.activitySpinner.stopAnimating()
                }
                return
            }
            
            DispatchQueue.main.async {
                this.navigationController!.dismiss(animated: true)
                Toast.show("Track recommended")
            }
        }
    }
    
    // Yeah this is kind of stupid. This user selection thing is pretty reusable and I've gone and messed it up by adding
    // review queue specific stuff in here. I'll fix it if I end up reusing this
    init(_ tracks: [Track]) {
        self.tracks = tracks
        
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class SelectEntityCell<T>: BasicEntityCell<T> {
    
    let leftImage: IconView = {
        let icon = IconView("checkmark", weight: .medium, scale: .medium)
        icon.translatesAutoresizingMaskIntoConstraints = false
        icon.tintColor = Colors.primary
        return icon
    }()
    
    var isActive: Bool {
        didSet {
            leftImage.isHidden = !isActive
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        isActive = false
        
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(leftImage)
                
        NSLayoutConstraint.activate([
            leftImage.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            leftImage.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            leftImage.widthAnchor.constraint(equalToConstant: 40),
            
            nameLabel.leadingAnchor.constraint(equalTo: leftImage.trailingAnchor),
        ])
    }
}

class SelectUserCell : SelectEntityCell<User> {
    var user: User? {
        set(user) {
            entity = user
        }
        get {
            entity
        }
    }
}

struct ReviewQueueRecommendRequest : Codable {
    let trackIds: [Int]
    let targetUserIds: [Int]
}
