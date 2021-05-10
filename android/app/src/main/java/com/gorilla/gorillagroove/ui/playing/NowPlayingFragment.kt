package com.gorilla.gorillagroove.ui.playing

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
class NowPlayingFragment : TrackListFragment<DbTrack>() {

    init {
        showFilterMenu = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Now Playing view")

        requireActivity().title_tv.text = "Now Playing"
    }

    override suspend fun loadTracks(): List<DbTrack> {
        return mainRepository.nowPlayingTracks
    }
}
