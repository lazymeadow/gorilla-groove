import UIKit
import CoreData

class MyLibraryController: UITableViewController {

    @IBOutlet weak var songButton: UIButton!
    
    let options = [
        ("Title", SongViewController()),
        ("Artist", nil),
        ("Album", nil)
    ]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "My Library"

        let view = self.view as! UITableView
        
        view.register(UITableViewCell.self, forCellReuseIdentifier: "libraryCell")
        
        // Remove extra table row lines that have no content
        view.tableFooterView = UIView(frame: .zero)
        
        self.navigationItem.leftBarButtonItem = UIBarButtonItem(
            title: "Logout",
            style: .plain,
            target: self,
            action: #selector(logout)
        )
    }
    
    override func viewDidAppear(_ animated: Bool) {
        UserSyncManager().postCurrentDevice()
        TrackState().syncWithServer()
    }
    
    @objc func logout(_ sender: Any) {
        // TODO actually send the logout command to the API
        LoginState.clear()
        AudioPlayer.stop()

        // Until we ditch the storyboard, have to navigate to the login view this way
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "LoginController")
        vc.modalPresentationStyle = .fullScreen
        vc.modalTransitionStyle = .crossDissolve
        self.present(vc, animated: true)
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return options.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "libraryCell", for: indexPath)
        
        cell.textLabel!.text = options[indexPath.row].0
        cell.textLabel!.textColor = #colorLiteral(red: 0.1764705926, green: 0.4980392158, blue: 0.7568627596, alpha: 1)
        let tapGesture = UITapGestureRecognizer(
            target: self,
            action: #selector(handleTap(sender:))
        )
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! UITableViewCell
        
        UIView.animate(withDuration: 0.12, animations: {
            cell.backgroundColor = SongViewCell.selectionColor
        }) { (finished) in
            UIView.animate(withDuration: 0.12, animations: {
                cell.backgroundColor = SongViewCell.normalColor
            })
        }
        
        let tapLocation = sender.location(in: self.tableView)
        
        let optionIndex = self.tableView.indexPathForRow(at: tapLocation)![1]
        let viewController = options[optionIndex].1
        
        if (viewController != nil) {
            self.navigationController!.pushViewController(options[optionIndex].1!, animated: true)
        }
    }
}
