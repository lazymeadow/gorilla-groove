package com.example.groove.db.model

import java.sql.Timestamp

interface RemoteSyncable {
	val id: Long

	val createdAt: Timestamp

	var updatedAt: Timestamp

	var deleted: Boolean

	// This is actually what gets returned to the mobile apps, with any cruft trimmed out
	fun toSyncDTO(): Any { return this }
}
