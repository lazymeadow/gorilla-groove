package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import com.gorilla.gorillagroove.database.entity.DbSyncStatus

@Dao
abstract class SyncStatusDao : BaseRoomDao<DbSyncStatus>("sync_status")
