package com.example.groove.db.dao

import com.example.groove.db.model.Track
import org.springframework.data.repository.CrudRepository

interface TrackRepository : CrudRepository<Track, Long>
