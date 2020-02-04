//
//  User+CoreDataProperties.swift
//  Gorilla Groove
//
//  Created by Ayrton Stout on 2/2/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//
//

import Foundation
import CoreData


extension User {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<User> {
        return NSFetchRequest<User>(entityName: "User")
    }

    @NSManaged public var last_sync: Date?
    @NSManaged public var id: Int64
    @NSManaged public var name: String
    @NSManaged public var last_login: Date
    @NSManaged public var created_at: Date

}
