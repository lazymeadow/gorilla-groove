package com.gorilla.gorillagroove.ui.playlists

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import com.gorilla.gorillagroove.model.PlaylistKey
import com.gorilla.gorillagroove.model.Track
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.ui.PlaylistsEvent
import com.gorilla.gorillagroove.ui.TrackListFragment
import com.gorilla.gorillagroove.util.Constants
import com.gorilla.gorillagroove.util.StateEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class PlaylistTrackFragment : TrackListFragment() {

    private lateinit var playlist: PlaylistKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playlist = arguments?.getSerializable("PLAYLIST") as? PlaylistKey
            ?: throw IllegalArgumentException("PLAYLIST key must be provided to the PlaylistFragment!")

        requireActivity().title_tv.text = playlist.name
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeObservers()

        viewModel.setPlaylistsEvent(PlaylistsEvent.GetPlaylist(playlist.id))
    }

    private fun subscribeObservers() {
        viewModel.playlist.observe(requireActivity(), Observer { playlistState ->
            when (playlistState.stateEvent) {
                is StateEvent.Success -> {
                    val tracks = playlistState.data?.playlistItems?.map { it.track } ?: run {
                        logError("Could not get playlist items from playlist! ID: ${playlistState.data?.id}")
                        emptyList<Track>()
                    }
                    trackCellAdapter.submitList(tracks)
                }
                is StateEvent.Error -> {
                    Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show()
                }
                is StateEvent.Loading -> {
                }
            }
        })
    }

    override fun onTrackClick(position: Int) {
        val clickedTrack = trackCellAdapter.filteredList[position]
        playerControlsViewModel.playMedia(clickedTrack, Constants.CALLING_FRAGMENT_PLAYLIST, playlist.id)
    }
}
