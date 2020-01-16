//
//  MyLibraryController.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/5/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import UIKit
import CoreData

class MyLibraryController: UITableViewController {

    let container = GroovePersistence()
    @IBOutlet weak var songButton: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        songButton.addTarget(self, action: #selector(MyLibraryController.showSongs), for: .touchUpInside)
        TrackState().syncWithServer()
        
//     let contactsTableView = UITableView() // view
        
        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
    }
    
    @IBAction func logout(_ sender: Any) {
        print("Logout")
        LoginState.clear()
        self.performSegue(withIdentifier: "logoutSegue", sender: nil)
    }
    
    @IBAction func showSongs() {
        print("Show songs")
        let vc = SongViewController()
        self.navigationController?.pushViewController(vc, animated: true)
    }
    // MARK: - Table view data source

    /*
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "reuseIdentifier", for: indexPath)

        // Configure the cell...

        return cell
    }
    */

    /*
    // Override to support conditional editing of the table view.
    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        // Return false if you do not want the specified item to be editable.
        return true
    }
    */

    /*
    // Override to support editing the table view.
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCellEditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            // Delete the row from the data source
            tableView.deleteRows(at: [indexPath], with: .fade)
        } else if editingStyle == .insert {
            // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
        }    
    }
    */

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
