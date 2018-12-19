package com.example.groove.db.dao

import com.example.groove.db.model.Playlist
import com.example.groove.db.model.PlaylistUser
import com.example.groove.db.model.User
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface PlaylistUserRepository : CrudRepository<PlaylistUser, Long> {

	@Query("SELECT pu.playlist " +
			"FROM PlaylistUser pu " +
			"WHERE pu.user = :user "
	)
	fun getPlaylistsForUser(
			@Param("user") user: User
	): List<Playlist>

}
