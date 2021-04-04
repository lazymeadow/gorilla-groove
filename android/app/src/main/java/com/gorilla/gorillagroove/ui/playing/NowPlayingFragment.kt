package com.gorilla.gorillagroove.ui.playing

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NowPlayingFragment : TrackListFragment() {

    @Inject
    lateinit var mainRepository: MainRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Now Playing view")
    }

    override fun onStart() {
        super.onStart()

        trackCellAdapter.submitList(mainRepository.nowPlayingTracks)
    }

    override fun onTrackClick(position: Int) {
        val clickedTrack = trackCellAdapter.filteredList[position]
//        playerControlsViewModel.playMedia(clickedTrack, Constants.CALLING_FRAGMENT_NOW_PLAYING)
    }
}
