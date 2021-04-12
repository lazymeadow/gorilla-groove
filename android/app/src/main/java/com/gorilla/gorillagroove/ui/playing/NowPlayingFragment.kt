package com.gorilla.gorillagroove.ui.playing

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NowPlayingFragment : TrackListFragment() {

    init {
        showFilterMenu = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Now Playing view")
    }

    override fun onStart() {
        super.onStart()

        trackCellAdapter.submitList(mainRepository.nowPlayingTracks)
    }
}
