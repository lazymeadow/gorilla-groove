import Foundation
import UIKit


class SettingsController : UITableViewController {

    var users: Array<User> = []
    
    override func viewDidLoad() {
        print("Loaded users")
        super.viewDidLoad()
        self.title = "Settings"

        let view = self.view as! UITableView
        
        view.register(UITableViewCell.self, forCellReuseIdentifier: "libraryCell")
        
        // Remove extra table row lines that have no content
        view.tableFooterView = UIView(frame: .zero)
        
        users = UserDao.getOtherUsers()
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 0
//        return users.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "libraryCell", for: indexPath)
        
//        cell.textLabel!.text = users[indexPath.row].name
//        cell.textLabel!.textColor = UIColor(named: "Table Text")
        
        return cell
    }
}
