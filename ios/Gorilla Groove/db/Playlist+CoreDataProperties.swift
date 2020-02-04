//
//  Playlist+CoreDataProperties.swift
//  Gorilla Groove
//
//  Created by Ayrton Stout on 2/2/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//
//

import Foundation
import CoreData


extension Playlist {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<Playlist> {
        return NSFetchRequest<Playlist>(entityName: "Playlist")
    }

    @NSManaged public var id: Int64
    @NSManaged public var user_id: Int64
    @NSManaged public var name: String
    @NSManaged public var created_at: Date?
    @NSManaged public var updated_at: Date?

}
