package com.example.groove.services

import com.example.groove.db.dao.*
import com.example.groove.db.model.*
import com.example.groove.db.model.enums.OwnershipType
import com.example.groove.exception.DataConflictException
import com.example.groove.exception.PermissionDeniedException
import com.example.groove.exception.ResourceNotFoundException
import com.example.groove.util.DateUtils.now
import com.example.groove.util.get
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.streams.toList


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
	fun renamePlaylist(user: User, playlistId: Long, name: String): Playlist {
		val playlist = playlistRepository.get(playlistId)
				?: throw IllegalArgumentException("No playlist with ID: $playlistId found")

		if (userCanEditPlaylist(user, playlist)) {
			playlist.name = name
			playlist.updatedAt = now()
		} else {
			throw PermissionDeniedException("User has insufficient privileges to view playlist with ID: $playlistId")
		}

		playlistRepository.save(playlist)

		return playlist
	}

	@Transactional
	fun createPlaylist(user: User, name: String): Playlist {
		val playlist = Playlist(name = name)
		val playlistUser = PlaylistUser(playlist = playlist, user = user, ownershipType = OwnershipType.OWNER)

		playlistRepository.save(playlist)
		playlistUserRepository.save(playlistUser)

		return playlist
	}

	@Synchronized
	@Transactional
	fun deletePlaylist(user: User, playlistId: Long) {
		logger.info("${user.email} is attempting to delete playlist $playlistId")

		val playlist = playlistRepository.get(playlistId)
				?: throw IllegalArgumentException("Playlist not found with ID $playlistId")
		val playlistUser = playlist.users.find { it.user.id == user.id }
				?: throw IllegalArgumentException("Playlist not found with ID $playlistId")

		if (playlistUser.ownershipType != OwnershipType.OWNER) {
			throw PermissionDeniedException("Must be a playlist's owner to delete it.")
		}

		playlist.deleted = true
		playlist.updatedAt = now()
		playlistRepository.save(playlist)

		playlistTrackRepository.softDeletePlaylistTracksByPlaylistIds(
				updatedAt = now(),
				ids = listOf(playlistId)
		)
	}

	@Transactional(readOnly = true)
	fun getTracks(
			name: String? = null,
			artist: String? = null,
			album: String? = null,
			playlistId: Long,
			searchTerm: String? = null,
			pageable: Pageable = Pageable.unpaged()
	): Page<PlaylistTrack> {
		val playlist = playlistRepository.get(playlistId)
				?: throw IllegalArgumentException("No playlist with ID: $playlistId found")
		playlistUserRepository.findByUserAndPlaylist(loadLoggedInUser(), playlist)
				?: throw PermissionDeniedException("User has insufficient privileges to view playlist with ID: $playlistId")

		return playlistTrackRepository.getTracks(name, artist, album, playlist, searchTerm, pageable)
	}

	@Synchronized
	@Transactional
	fun reorderPlaylist(playlistId: Long, playlistTrackIds: List<Long>) {
		// Make sure we are given unique entries
		val uniquePlaylistTrackIds = playlistTrackIds.toSet()
		if (uniquePlaylistTrackIds.size != playlistTrackIds.size) {
			throw IllegalArgumentException("All PlaylistTrack IDs must be unique")
		}

		val user = loadLoggedInUser()
		val playlist = playlistRepository.get(playlistId)
				?: throw ResourceNotFoundException("No playlist found with ID $playlistId")

		val playlistTracks = playlistTrackRepository.findAllByIdOrdered(playlistTrackIds)
		val playlistTrackIdsNotFound = uniquePlaylistTrackIds - playlistTracks.map { it.id }.toSet()
		if (playlistTrackIdsNotFound.isNotEmpty()) {
			throw DataConflictException("Not all PlaylistTrack ID were found. Did not find the following PlaylistTracks: $playlistTrackIdsNotFound")
		}

		if (!userCanEditPlaylist(user, playlist)) {
			logger.warn("User of ID ${user.id} tried to reorder the playlist of ID ${playlist.id} but did not have the permission to do so")
			throw PermissionDeniedException("Insufficient privileges to reorder the tracks of playlist ${playlist.id}")
		}

		playlistTracks.forEach { playlistTrack ->
			require(playlistTrack.playlist.id == playlistId) {
				"PlaylistTrack ${playlistTrack.id} is on playlist: ${playlistTrack.playlist.id}, not the expected playlist: $playlistId"
			}
			require(!playlistTrack.deleted) {
				"PlaylistTrack ${playlist.id} is deleted"
			}
		}

		// Permissions are now all good. Reordering playlist tracks expects ALL tracks to be provided to the endpoint.
		// There's probably a better way to do it. But this is easy. And I'm lazy. As a result of this, we need to
		// make sure that there is a playlist track for every track on said playlist or we should not proceed.
		val existingPlaylistTrackIds = playlistTrackRepository.getTracks(playlist = playlist)
				.get()
				.map { it.id }
				.toList()
				.toSet()

		val unexpectedIds = uniquePlaylistTrackIds - existingPlaylistTrackIds
		if (unexpectedIds.isNotEmpty()) {
			throw DataConflictException("Some PlaylistTrack IDs were provided that were unexpected. Have the tracks they reference been deleted? Unexpected PlaylistTracks: $unexpectedIds")
		}

		val missingIds = existingPlaylistTrackIds - uniquePlaylistTrackIds
		if (missingIds.isNotEmpty()) {
			throw DataConflictException("Not all PlaylistTrack IDs were provided. Expected the following PlaylistTracks: $missingIds")
		}

		// Ok they gave us good data. It's a miracle. Let's iterate through the tracks in the order that they gave them
		// to us, assign the order based off their index, and save them all
		playlistTracks.forEachIndexed { i, playlistTrack ->
			// There isn't an easy way to batch these in an efficient manner. So it's important to only call save
			// if we actually need to. Otherwise we might issue 100+ updates to the DB because the user reordered
			// one thing. Though if they moved something from the bottom to the top, then that'll happen anyway.
			// It's kind of unfortunate, and I'm not really sure of a good way around it other than introducing gaps
			// in the numbers so that one thing moving doesn't require everything to get bumped down. But that's going
			// to complicate logic a lot. So I'm leaving my thoughts here as a possible optimization if it becomes an issue.
			if (playlistTrack.sortOrder != i) {
				playlistTrack.sortOrder = i
				playlistTrack.updatedAt = now()
				playlistTrackRepository.save(playlistTrack)
			}
		}
	}

	@Synchronized
	@Transactional
	fun addTracksToPlaylists(playlistIds: List<Long>, trackIds: List<Long>): List<PlaylistTrack> {
		val user = loadLoggedInUser()
		val playlists = playlistIds.map { id ->
			playlistRepository.get(id) ?: throw IllegalArgumentException("Playlist ID: $id not found")
		}

		// Playlists can technically have multiple users on them. Not super sure how to handle private tracks.
		// If a user requested track links for a private track on their playlist, it would not work.
		// For now, only let a user add tracks if they are their own.
		val tracks = trackIds.map {
			val track = trackRepository.get(it)
			if (track == null || track.user.id != user.id || track.deleted) {
				throw IllegalArgumentException("Track ID: $it not found")
			}
			track
		}

		playlists.forEach { playlist ->
			if (!userCanEditPlaylist(user, playlist)) {
				logger.warn("User of ID ${user.id} tried to add tracks to the playlist of ID ${playlist.id} but did not have the permission to do so")
				throw PermissionDeniedException("Insufficient privileges to add the selected tracks to the playlist ${playlist.id}")
			}
		}

		val playlistTracks = playlists.flatMap { playlist ->
			// All methods that modify playlist tracks are synchronized, so this is safe to do.
			// No danger of creating duplicate order
			val currentHighestOrder = playlistTrackRepository.getHighestSortOrderOnPlaylist(playlist.id) ?: -1
			tracks.mapIndexed { i, track ->
				PlaylistTrack(playlist = playlist, track = track, sortOrder = currentHighestOrder + i + 1)
			}
		}

		playlistTrackRepository.saveAll(playlistTracks)

		return playlistTracks
	}

	// When we delete tracks, we want to make sure the playlist tracks are also deleted.
	@Synchronized
	@Transactional
	fun deletePlaylistTracksForTracks(user: User, tracks: List<Track>) {
		tracks.forEach { track ->
			if (track.user.id != user.id) {
				logger.warn("${user.name} attempted to delete playlist tracks with a track they did not own. ID: ${track.id}")
				throw IllegalArgumentException("Deleting playlist tracks for a track you do not own is unsupported")
			}
		}

		playlistTrackRepository.softDeletePlaylistTracksByTrackIds(
				updatedAt = now(),
				ids = tracks.map { it.id }
		)
	}

	@Synchronized
	@Transactional
	fun deletePlaylistTracks(playlistTrackIds: List<Long>) {
		val user = loadLoggedInUser()

		val playlistTracks = playlistTrackIds.map {
			val playlistTrack = playlistTrackRepository.get(it)
			if (playlistTrack == null || playlistTrack.track.user.id != user.id) {
				throw IllegalArgumentException("PlaylistTrack ID: $it not found")
			}
			playlistTrack
		}

		// We can get the ID of something without the JPA doing a query. So aggregate all the like IDs
		// and we can fetch the playlists in a single query
		val playlistIds = playlistTracks.map { it.playlist.id }.toSet()
		val playlists = playlistRepository.findAllById(playlistIds)

		playlists.forEach { playlist ->
			if (!userCanEditPlaylist(user, playlist)) {
				logger.warn("User of ID ${user.id} tried to remove tracks from the playlist of ID ${playlist.id} " +
						"but did not have the permission to do so")
				throw PermissionDeniedException("Insufficient privileges to remove the selected tracks from the playlist")
			}
		}

		// Now we know all permissions check out
		playlistTrackRepository.softDeleteByIds(updatedAt = now(), ids = playlistTrackIds)
	}

	private fun userCanEditPlaylist(user: User, playlist: Playlist): Boolean {
		val playlistUser = playlistUserRepository.findByUserAndPlaylist(user, playlist)

		return playlistUser?.ownershipType?.hasWritePrivilege == true
	}

	companion object {
		val logger = logger()
	}
}
