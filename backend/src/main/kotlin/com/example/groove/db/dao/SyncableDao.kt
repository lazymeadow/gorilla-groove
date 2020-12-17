package com.example.groove.db.dao

import java.sql.Timestamp

interface RemoteSyncableDao {
	fun getLastModifiedRow(userId: Long): Timestamp
}