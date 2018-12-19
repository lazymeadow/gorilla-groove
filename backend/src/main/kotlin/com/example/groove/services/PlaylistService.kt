package com.example.groove.services

import com.example.groove.db.dao.PlaylistRepository
import com.example.groove.db.dao.PlaylistUserRepository
import com.example.groove.db.model.Playlist
import com.example.groove.db.model.PlaylistUser
import com.example.groove.db.model.User
import com.example.groove.db.model.enums.OwnershipType
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class PlaylistService(
		private val playlistRepository: PlaylistRepository,
		private val playlistUserRepository: PlaylistUserRepository
) {

	@Transactional(readOnly = true)
	fun getPlaylists(user: User): List<Playlist> {
		return playlistUserRepository.getPlaylistsForUser(user)
	}

	fun createPlaylist(user: User, name: String): Playlist {
		val playlist = Playlist(name = name)
		val playlistUser = PlaylistUser(playlist = playlist, user = user, ownershipType = OwnershipType.OWNER)

		playlistRepository.save(playlist)
		playlistUserRepository.save(playlistUser)

		return playlist
	}

	companion object {
		val logger = LoggerFactory.getLogger(PlaylistService::class.java)!!
	}
}
