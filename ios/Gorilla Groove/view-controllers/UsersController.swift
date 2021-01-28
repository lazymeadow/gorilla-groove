import Foundation
import UIKit


class UsersController : UITableViewController {

    private var users: [User] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Users"

        let view = self.view as! UITableView
        
        view.register(UserCell.self, forCellReuseIdentifier: "userCell")
        
        view.tableFooterView = UIView(frame: .zero)
        
        users = UserDao.getOtherUsers()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        GGNavLog.info("Loaded users view")
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return users.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "userCell", for: indexPath) as! UserCell
        
        cell.user = users[indexPath.row]
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! UserCell
        
        cell.animateSelectionColor()
        
        let user = cell.user!
        
        /// https://gorillagroove.net/api/track?userId=6&showHidden=true&sort=artist,DESC&sort=album,ASC&sort=trackNumber,ASC&size=75&page=0
        let vc = TrackViewController(user.name, originalView: .USER, userId: user.id)
        
        vc.modalPresentationStyle = .fullScreen
        self.navigationController!.pushViewController(vc, animated: true)
    }
}

class UserCell: UITableViewCell {
    
    var user: User? {
        didSet {
            guard let user = user else { return }
            nameLabel.text = user.name
        }
    }
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 16)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        contentView.addSubview(nameLabel)
         
        NSLayoutConstraint.activate([
            nameLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            nameLabel.leftAnchor.constraint(equalTo: contentView.leftAnchor, constant: 6),
            nameLabel.rightAnchor.constraint(equalTo: contentView.rightAnchor, constant: -6),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}

struct LiveTrackRequest : Codable {
    let content: [TrackResponse]
}
