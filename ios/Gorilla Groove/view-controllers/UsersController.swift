import Foundation
import UIKit


class UsersController : UITableViewController {

    var users: Array<User> = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Users"

        let view = self.view as! UITableView
        
        view.register(UITableViewCell.self, forCellReuseIdentifier: "libraryCell")
        
        // Remove extra table row lines that have no content
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
        let cell = tableView.dequeueReusableCell(withIdentifier: "libraryCell", for: indexPath)
        
        cell.textLabel!.text = users[indexPath.row].name
        cell.textLabel!.textColor = Colors.tableText
        
        return cell
    }
}
