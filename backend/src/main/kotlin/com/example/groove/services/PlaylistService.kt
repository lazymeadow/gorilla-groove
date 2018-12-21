package com.example.groove.services

import com.example.groove.db.dao.PlaylistRepository
import com.example.groove.db.dao.PlaylistTrackRepository
import com.example.groove.db.dao.PlaylistUserRepository
import com.example.groove.db.dao.TrackRepository
import com.example.groove.db.model.*
import com.example.groove.db.model.enums.OwnershipType
import com.example.groove.util.unwrap
import org.slf4j.LoggerFactory
import org.springframework.dao.PermissionDeniedDataAccessException

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class PlaylistService(
		private val playlistRepository: PlaylistRepository,
		private val trackRepository: TrackRepository,
		private val playlistUserRepository: PlaylistUserRepository,
		private val playlistTrackRepository: PlaylistTrackRepository
) {

	@Transactional(readOnly = true)
	fun getPlaylists(user: User): List<Playlist> {
		return playlistUserRepository.getPlaylistsForUser(user)
	}

	@Transactional
	fun createPlaylist(user: User, name: String): Playlist {
		val playlist = Playlist(name = name)
		val playlistUser = PlaylistUser(playlist = playlist, user = user, ownershipType = OwnershipType.OWNER)

		playlistRepository.save(playlist)
		playlistUserRepository.save(playlistUser)

		return playlist
	}

	@Transactional
	fun addTracksToPlaylist(user: User, playlistId: Long, trackIds: List<Long>) {
		val playlist = playlistRepository.findById(playlistId)
				.orElseThrow { IllegalArgumentException("Playlist ID: $playlistId not found") }

		val tracks = trackIds.map {
			trackRepository.findById(it)
					.orElseThrow { IllegalArgumentException("Track ID: $it not found") }
		}

		if (playlist == null || !userCanEditPlaylist(user, playlist, tracks)) {
			logger.warn("User of ID ${user.id} tried to add tracks to the playlist of ID ${playlist.id} but did not have the permission to do so")
			throw IllegalArgumentException("Insufficient privileges to add the selected tracks to the playlist")
		}

		tracks.forEach {
			playlistTrackRepository.save(PlaylistTrack(playlist = playlist, track = it))
		}
	}

	private fun userCanEditPlaylist(user: User, playlist: Playlist, tracks: List<Track>): Boolean {
		val playlistUser = playlistUserRepository.findByUserAndPlaylist(user, playlist)

		if (playlistUser == null || !playlistUser.ownershipType.hasWritePrivilege) {
			return false
		}

		return tracks.all { it.user == user }
	}

	companion object {
		val logger = LoggerFactory.getLogger(PlaylistService::class.java)!!
	}
}
