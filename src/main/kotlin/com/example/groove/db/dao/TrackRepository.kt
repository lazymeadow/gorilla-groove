package com.example.groove.db.dao

import com.example.groove.db.model.User
import org.springframework.data.repository.CrudRepository

interface TrackRepository : CrudRepository<User, Long>