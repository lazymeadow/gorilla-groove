package com.example.groove.db.model

import java.sql.Timestamp

interface RemoteSyncable {
	val id: Long

	val createdAt: Timestamp

	var updatedAt: Timestamp

	var deleted: Boolean
}