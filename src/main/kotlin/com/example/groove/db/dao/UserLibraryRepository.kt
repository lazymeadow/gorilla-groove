package com.example.groove.db.dao

import com.example.groove.db.model.Track
import com.example.groove.db.model.User
import com.example.groove.db.model.UserLibrary
import org.springframework.data.repository.CrudRepository

interface UserLibraryRepository : CrudRepository<UserLibrary, Long> {
	fun findByTrack(track: Track): List<UserLibrary>
	fun findByTrackAndUser(track: Track, user: User): UserLibrary?
}
