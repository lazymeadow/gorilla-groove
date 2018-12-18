package com.example.groove.db.dao

import com.example.groove.db.model.Playlist
import org.springframework.data.repository.CrudRepository

interface PlaylistRepository : CrudRepository<Playlist, Long> {

}
