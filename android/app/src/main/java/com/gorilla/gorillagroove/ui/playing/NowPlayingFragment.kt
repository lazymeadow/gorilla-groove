package com.gorilla.gorillagroove.ui.playing

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.gorilla.gorillagroove.ui.NowPlayingEvent
import com.gorilla.gorillagroove.ui.TrackListFragment
import com.gorilla.gorillagroove.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class NowPlayingFragment : TrackListFragment() {

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeObservers()
        viewModel.setNowPlayingEvent(NowPlayingEvent.GetNowPlayingTracksEvent)
    }

    private fun subscribeObservers() {
        viewModel.nowPlayingTracks.observe(requireActivity(), Observer {
            trackCellAdapter.submitList(it)
            trackCellAdapter.notifyDataSetChanged()
        })
    }

    override fun onTrackClick(position: Int) {
        val clickedTrack = trackCellAdapter.filteredList[position]
        playerControlsViewModel.playMedia(clickedTrack, Constants.CALLING_FRAGMENT_NOW_PLAYING)
    }
}
