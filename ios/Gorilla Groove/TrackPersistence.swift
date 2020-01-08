//
//  TrackPersistence.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/7/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import CoreData

class TrackPersistence: NSPersistentContainer {
    
    convenience init() {
        self.init(name: "Track")
    }
    
    func saveContext(backgroundContext: NSManagedObjectContext? = nil) {
        print("Saving Tracks maybe?")
        let context = backgroundContext ?? viewContext
        guard context.hasChanges else { return }
        do {
            try context.save()
        } catch let error as NSError {
            print("Error: \(error), \(error.userInfo)")
        }
    }
}
