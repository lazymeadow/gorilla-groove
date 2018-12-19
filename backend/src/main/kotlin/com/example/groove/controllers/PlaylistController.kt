package com.example.groove.controllers

import com.example.groove.db.model.Playlist
import com.example.groove.services.PlaylistService
import com.example.groove.util.loadLoggedInUser

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

	data class CreatePlaylistDTO(
			val name: String
	)
}
