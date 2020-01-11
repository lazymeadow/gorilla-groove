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
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        print("Load new stuff")
        

        print("About to print")
        let coreDataManager = CoreDataManager(modelName: "Groove")
        let context = coreDataManager.managedObjectContext
        
        /*
        let entity = NSEntityDescription.entity(forEntityName: "Track", in: context)
        let newTrack = NSManagedObject(entity: entity!, insertInto: context)
        newTrack.setValue("behst name", forKey: "name")
        newTrack.setValue("bgest name", forKey: "album")
                newTrack.setValue("best name1", forKey: "artist")
                newTrack.setValue("best name2", forKey: "featuring")
                newTrack.setValue("best nam3e", forKey: "genre")
                newTrack.setValue("best name4", forKey: "note")
                newTrack.setValue(Date(), forKey: "created_at")
                newTrack.setValue(Date(), forKey: "updated_at")
                newTrack.setValue(2007, forKey: "release_year")
                newTrack.setValue(1, forKey: "track_number")
                newTrack.setValue(42, forKey: "id")
                newTrack.setValue(false, forKey: "is_hidden")
                newTrack.setValue(false, forKey: "is_private")
                newTrack.setValue(Date(), forKey: "last_played")
                newTrack.setValue(187, forKey: "length")
                newTrack.setValue(0, forKey: "play_count")
        
        do {
            try context.save()
        } catch {
            print("Couldn't save")
        }
 */
        
        
        do {
            let request = NSFetchRequest<NSFetchRequestResult>(entityName: "Track")
            let result = try context.fetch(request)
            print(result)
            for data in result as! [NSManagedObject] {
                print(data)
//                let userName = data.value(forKey: "username") as! String
//                let age = data.value(forKey: "age") as! String
//                print("User Name is : "+userName+" and Age is : "+age)
            }
        } catch {
            print("Fetching data Failed")
        }
//        try! managedObjectContext.save()
        
        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
    }
    
    @IBAction func logout(_ sender: Any) {
        print("Logout")
        LoginState.clear()
        self.performSegue(withIdentifier: "logoutSegue", sender: nil)
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
    // Override to support rearranging the table view.
    override func tableView(_ tableView: UITableView, moveRowAt fromIndexPath: IndexPath, to: IndexPath) {

    }
    */

    /*
    // Override to support conditional rearranging of the table view.
    override func tableView(_ tableView: UITableView, canMoveRowAt indexPath: IndexPath) -> Bool {
        // Return false if you do not want the item to be re-orderable.
        return true
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
