package com.example.groove.db.dao

import com.example.groove.db.model.TrackHistory
import org.springframework.data.repository.CrudRepository

interface TrackHistoryRepository : CrudRepository<TrackHistory, Long>
