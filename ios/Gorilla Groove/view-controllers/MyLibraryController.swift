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

    @IBOutlet weak var songButton: UIButton!
    
    let options = [
        ("Song", SongViewController()),
        ("Artist", nil),
        ("Album", nil)
    ]
    
    override func viewDidLoad() {
        super.viewDidLoad()

        TrackState().syncWithServer()
        
//        self.view.heightAnchor.constraint(equalToConstant: 150).isActive = true
//        self.view.widthAnchor.constraint(equalToConstant: 150).isActive = true
        
        (self.view as! UITableView).register(UITableViewCell.self, forCellReuseIdentifier: "libraryCell")
    }
    
    @IBAction func logout(_ sender: Any) {
        print("Logout")
        LoginState.clear()
        self.performSegue(withIdentifier: "logoutSegue", sender: nil)
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return options.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "libraryCell", for: indexPath)
        
        cell.textLabel!.text = options[indexPath.row].0
        let tapGesture = UITapGestureRecognizer(
            target: self,
            action: #selector(handleTap(sender:))
        )
        cell.addGestureRecognizer(tapGesture)
        
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! UITableViewCell
        
        let tapLocation = sender.location(in: self.tableView)
        
        let optionIndex = self.tableView.indexPathForRow(at: tapLocation)![1]
        let viewController = options[optionIndex].1
        
        if (viewController != nil) {
            self.navigationController!.pushViewController(options[optionIndex].1!, animated: true)
        }
    }
}
