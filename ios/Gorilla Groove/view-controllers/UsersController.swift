import Foundation
import UIKit


class UsersController : UITableViewController {

    let userState = UserState()
    var users: Array<User> = []
    
    override func viewDidLoad() {
        print("Loaded users")
        super.viewDidLoad()
        self.title = "Users"

        let view = self.view as! UITableView
        
        view.register(UITableViewCell.self, forCellReuseIdentifier: "libraryCell")
        
        // Remove extra table row lines that have no content
        view.tableFooterView = UIView(frame: .zero)
        
        users = userState.getOtherUsers()
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return users.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "libraryCell", for: indexPath)
        
        cell.textLabel!.text = users[indexPath.row].name
        cell.textLabel!.textColor = #colorLiteral(red: 0.1764705926, green: 0.4980392158, blue: 0.7568627596, alpha: 1)
        
        return cell
    }
}
