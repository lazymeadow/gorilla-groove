import Foundation
import UIKit


class SettingsController : UITableViewController {

    var settings: Dictionary<String, String> = [:]
    
    override func viewDidLoad() {
        print("Loaded users")
        super.viewDidLoad()
        self.title = "Settings"

        let view = self.view as! UITableView
        
        view.register(TableViewCell<Any>.self, forCellReuseIdentifier: "settingsCell")
        
        // Remove extra table row lines that have no content
        view.tableFooterView = UIView(frame: .zero)
        
        settings = [:
//            "Broadcast Played Song": "always"
        ]
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return settings.count
//        return users.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "settingsCell", for: indexPath)
        
//        let switchView = UISwitch(frame: .zero)
//        switchView.setOn(false, animated: true)
//        switchView.tag = indexPath.row // for detect which row switch Changed
//        switchView.addTarget(self, action: #selector(self.switchChanged(_:)), for: .valueChanged)
        
        let label = UILabel()
        label.text = "Test >"
        label.sizeToFit()
        
        cell.accessoryView = label
        cell.textLabel!.text = "Offline Song Storage"
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dropdownSelected(sender:)))
        cell.addGestureRecognizer(tapGesture)
//        cell.textLabel!.text = users[indexPath.row].name
//        cell.textLabel!.textColor = UIColor(named: "Table Text")
        
        return cell
    }
    
    @objc func dropdownSelected(sender: UITapGestureRecognizer) {
        let cell = sender.view as! UITableViewCell
        let tableIndex = tableView.indexPath(for: cell)!
//        let track = visibleTracks[tableIndex.row]

        let alert = createMenuForOfflineStorage() {
            
        }
        
        ViewUtil.showAlert(alert)
    }
    
    private func createMenuForOfflineStorage(onAction: @escaping () -> Void) -> UIAlertController {
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        alert.addAction(UIAlertAction(title: "All Tracks", style: .default, handler: { _ in
            
        }))
        alert.addAction(UIAlertAction(title: "Explicit Offline Only", style: .default, handler: { _ in
            
        }))
        alert.addAction(UIAlertAction(title: "None", style: .default, handler: { _ in
            
        }))
        
        return alert
    }
    
}

    
