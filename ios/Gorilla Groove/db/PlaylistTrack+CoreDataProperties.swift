//
//  PlaylistTrack+CoreDataProperties.swift
//  Gorilla Groove
//
//  Created by Ayrton Stout on 2/2/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//
//

import Foundation
import CoreData


extension PlaylistTrack {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<PlaylistTrack> {
        return NSFetchRequest<PlaylistTrack>(entityName: "PlaylistTrack")
    }

    @NSManaged public var id: Int64
    @NSManaged public var playlist_id: Int64
    @NSManaged public var track_id: Int64
    @NSManaged public var created_at: Date?

}
