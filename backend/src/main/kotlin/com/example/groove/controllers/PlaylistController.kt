package com.example.groove.controllers

import com.example.groove.db.model.Playlist
import com.example.groove.db.model.PlaylistTrack
import com.example.groove.services.PlaylistService
import com.example.groove.util.loadLoggedInUser
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
		return playlistService.createPlaylist(loadLoggedInUser(), createPlaylistDTO.name)
    }

	@PutMapping("/{playlistId}")
	fun editPlaylist(
			@PathVariable playlistId: Long,
			@RequestBody createPlaylistDTO: CreatePlaylistDTO
	): Playlist {
		return playlistService.renamePlaylist(loadLoggedInUser(), playlistId, createPlaylistDTO.name)
	}

	@DeleteMapping("/{playlistId}")
	fun deletePlaylist(@PathVariable playlistId: Long) {
		return playlistService.deletePlaylist(loadLoggedInUser(), playlistId)
	}

	@PostMapping("/track")
	fun addToPlaylist(@RequestBody addPlaylistTrackDTO: AddPlaylistTrackDTO) {
		playlistService.addTracksToPlaylist(addPlaylistTrackDTO.playlistId, addPlaylistTrackDTO.trackIds)
	}

	@DeleteMapping("/track")
	fun removeFromPlaylist(@RequestBody removePlaylistTrackDTO: RemovePlaylistTrackDTO) {
		playlistService.deletePlaylistTracks(removePlaylistTrackDTO.playlistTrackIds)
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

	data class CreatePlaylistDTO(
			val name: String
	)

	data class AddPlaylistTrackDTO(
			val playlistId: Long,
			val trackIds: List<Long>
	)

	data class RemovePlaylistTrackDTO(
			val playlistTrackIds: List<Long>
	)
}
