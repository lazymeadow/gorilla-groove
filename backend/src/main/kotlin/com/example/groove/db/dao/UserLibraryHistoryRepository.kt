package com.example.groove.db.dao

import com.example.groove.db.model.UserLibraryHistory
import org.springframework.data.repository.CrudRepository

interface UserLibraryHistoryRepository : CrudRepository<UserLibraryHistory, Long>
