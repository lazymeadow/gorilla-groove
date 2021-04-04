package com.gorilla.gorillagroove.ui.playlists

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class PlaylistTrackFragment : TrackListFragment() {

    private lateinit var playlist: DbPlaylist

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playlist = arguments?.getSerializable("PLAYLIST") as? DbPlaylist
            ?: throw IllegalArgumentException("A DbPlaylist with key 'PLAYLIST' must be provided to the PlaylistFragment!")

        requireActivity().title_tv.text = playlist.name
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading PlaylistTracks view with playlist ID: ${playlist.id}")

//        viewModel.setPlaylistsEvent(PlaylistsEvent.GetPlaylist(playlist.id))
    }

    override fun onTrackClick(position: Int) {
        val clickedTrack = trackCellAdapter.filteredList[position]
//        playerControlsViewModel.playMedia(clickedTrack, Constants.CALLING_FRAGMENT_PLAYLIST, playlist.id)
    }
}
