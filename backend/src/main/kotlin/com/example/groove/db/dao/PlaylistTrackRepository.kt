package com.example.groove.db.dao

import com.example.groove.db.model.PlaylistTrack
import org.springframework.data.jpa.repository.JpaRepository

interface PlaylistTrackRepository : JpaRepository<PlaylistTrack, Long>
