import Foundation
import UIKit


class BaseUsersController<T: UserCell> : UIViewController, UITableViewDataSource, UITableViewDelegate {

    private var users: [User] = []
    private var visibleUsers: [User] = []
    private var showInactiveUsers = false
    
    private let tableView = UITableView()
    
    private lazy var filterOptions: [[FilterOption]] = [[
        FilterOption("Show Inactive") { [weak self] option in
            guard let this = self else { return }
            
            this.showInactiveUsers = !this.showInactiveUsers
            option.filterImage = this.showInactiveUsers ? .CHECKED : .NONE
            
            this.filter.reloadData()
            this.setVisibleUsers()
        }
    ]]
    
    private lazy var filter = TableFilter(filterOptions, vc: self)
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Users"
        
        tableView.register(T.self, forCellReuseIdentifier: "userCell")

        view.addSubview(tableView)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leftAnchor.constraint(equalTo: view.leftAnchor),
            tableView.rightAnchor.constraint(equalTo: view.rightAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            filter.topAnchor.constraint(equalTo: view.topAnchor),
            filter.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -10),
        ])
        
        tableView.dataSource = self
        tableView.delegate = self
        
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        users = UserDao.getOtherUsers()
        setVisibleUsers()
    }
    
    private func setVisibleUsers() {
        if showInactiveUsers {
            visibleUsers = users
        } else {
            let now = Date()

            visibleUsers = users.filter { user in
                if let lastLogin = user.lastLogin {
                    return Calendar.current.dateComponents([.day], from: lastLogin, to: now).day! < 45
                } else {
                    return false
                }
            }
        }
        
        tableView.reloadData()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return visibleUsers.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "userCell", for: indexPath) as! T
        
        cell.user = visibleUsers[indexPath.row]
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    // Needs to be subclassed. Swift doesn't support abstract functions. Very classy
    @objc func handleTap(sender: UITapGestureRecognizer) { }
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
            nameLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -6),
        ])
        
        let leadingConstraint = nameLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 6)
        leadingConstraint.priority = UILayoutPriority(250)
        leadingConstraint.isActive = true
    }
    
    @available(*, unavailable)
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

