package com.example.groove.controllers

import com.example.groove.db.model.Playlist
import com.example.groove.db.model.Track
import com.example.groove.services.PlaylistService
import com.example.groove.util.loadLoggedInUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("playlist")
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

	@PostMapping("/track")
	fun addToPlaylist(@RequestBody addToPlaylistDTO: AddToPlaylistDTO) {
		playlistService.addTracksToPlaylist(loadLoggedInUser(), addToPlaylistDTO.playlistId, addToPlaylistDTO.trackIds)
	}

	@GetMapping("/track")
    fun getPlaylistTracks(
			@RequestParam(value = "playlistId") playlistId: Long,
			@RequestParam(value = "name") name: String?,
			@RequestParam(value = "artist") artist: String?,
			@RequestParam(value = "album") album: String?,
			pageable: Pageable
	): Page<Track> {
		return playlistService.getTracks(name, artist, album, playlistId, pageable)
    }

	data class CreatePlaylistDTO(
			val name: String
	)

	data class AddToPlaylistDTO(
			val playlistId: Long,
			val trackIds: List<Long>
	)
}
