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
    @NSManaged public var user_id: Int64

}
