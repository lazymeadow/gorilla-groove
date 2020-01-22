//
//  UserSync+CoreDataProperties.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/11/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//
//

import Foundation
import CoreData


extension UserSync {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<UserSync> {
        return NSFetchRequest<UserSync>(entityName: "UserSync")
    }

    @NSManaged public var last_sync: NSDate
    @NSManaged public var device_id: UUID
    @NSManaged public var user_id: Int64
    
    // Don't have to actually lowercase this UUID for any real reason, but the other devices do it lower and I'm OCD
    func deviceIdAsString() -> String {
        return device_id.uuidString.lowercased()
    }
}
