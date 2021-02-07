package com.example.groove.controllers

import com.example.groove.db.model.Playlist
import com.example.groove.db.model.PlaylistTrack
import com.example.groove.services.PlaylistService
import com.example.groove.util.loadLoggedInUser
import com.example.groove.util.logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/playlist")
class PlaylistController(
		private val playlistService: PlaylistService
) {

	@GetMapping
    fun getPlaylists(): List<Playlist> {
		return playlistService.getPlaylists(loadLoggedInUser())
    }

	@PostMapping
    fun createPlaylist(@RequestBody createPlaylistDTO: CreatePlaylistDTO): Playlist {
		val user = loadLoggedInUser()
		logger.info("User ${user.name} is creating a playlist with name ${createPlaylistDTO.name}")
		return playlistService.createPlaylist(user, createPlaylistDTO.name)
    }

	@PutMapping("/{playlistId}")
	fun editPlaylist(
			@PathVariable playlistId: Long,
			@RequestBody createPlaylistDTO: CreatePlaylistDTO
	): Playlist {
		val user = loadLoggedInUser()
		logger.info("User ${user.name} is editing playlist $playlistId")
		return playlistService.renamePlaylist(user, playlistId, createPlaylistDTO.name)
	}

	@DeleteMapping("/{playlistId}")
	fun deletePlaylist(@PathVariable playlistId: Long) {
		val user = loadLoggedInUser()
		logger.info("User ${user.name} is deleting playlist $playlistId")
		return playlistService.deletePlaylist(user, playlistId)
	}

	@PostMapping("/track")
	fun addToPlaylist(@RequestBody addPlaylistTrackDTO: AddPlaylistTrackDTO): PlaylistTrackResponse {
		logger.info("User ${loadLoggedInUser().name} is adding tracks ${addPlaylistTrackDTO.trackIds} to playlists ${addPlaylistTrackDTO.playlistIds}")
		val newPlaylistTracks = playlistService.addTracksToPlaylists(addPlaylistTrackDTO.playlistIds, addPlaylistTrackDTO.trackIds)
		return PlaylistTrackResponse(items = newPlaylistTracks)
	}

	@DeleteMapping("/track")
	fun removeFromPlaylist(@RequestParam playlistTrackIds: List<Long>) {
		require(playlistTrackIds.isNotEmpty()) {
			"A playlistTrackId is required"
		}

		logger.info("User ${loadLoggedInUser().name} is removing playlist tracks $playlistTrackIds")
		playlistService.deletePlaylistTracks(playlistTrackIds)
	}

	@GetMapping("/track")
    fun getPlaylistTracks(
			@RequestParam(value = "playlistId") playlistId: Long,
			@RequestParam(value = "name") name: String?,
			@RequestParam(value = "artist") artist: String?,
			@RequestParam(value = "album") album: String?,
			@RequestParam(value = "searchTerm") searchTerm: String?,
			pageable: Pageable
	): Page<PlaylistTrack> {
		return playlistService.getTracks(name, artist, album, playlistId, searchTerm, pageable)
    }

	@PutMapping("/track/sort-order")
	fun setPlaylistTrackOrder(
		@RequestBody body: ReorderPlaylistRequest
	) {
		logger.info("User ${loadLoggedInUser().name} is reordering playlist ${body.playlistId}")
		return playlistService.reorderPlaylist(body.playlistId, body.playlistTrackIds)
	}

	data class CreatePlaylistDTO(
			val name: String
	)

	data class AddPlaylistTrackDTO(
			val playlistIds: List<Long>,
			val trackIds: List<Long>
	)

	data class ReorderPlaylistRequest(
			val playlistId: Long,
			val playlistTrackIds: List<Long>
	)

	data class PlaylistTrackResponse(
			val items: List<PlaylistTrack>
	)

	companion object {
		private val logger = logger()
	}
}
