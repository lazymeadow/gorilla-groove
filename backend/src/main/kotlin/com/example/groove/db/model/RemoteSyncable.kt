package com.example.groove.db.model

import java.sql.Timestamp

interface RemoteSyncable : SyncDTO {
	override val id: Long

	val createdAt: Timestamp

	var updatedAt: Timestamp

	var deleted: Boolean

	// This is actually what gets returned to the mobile apps, with any cruft trimmed out
	fun toSyncDTO(): SyncDTO { return this }
}

interface SyncDTO {
	val id: Long
}
