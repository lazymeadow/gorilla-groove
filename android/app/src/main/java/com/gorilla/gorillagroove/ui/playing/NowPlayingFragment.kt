package com.gorilla.gorillagroove.ui.playing

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_track_list.*
import kotlin.math.min

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

    override fun onTracksLoaded() {
        super.onTracksLoaded()

        track_rv.post {
            // Kind of dumb, but there isn't an EASY way to scroll an item to the center of a recyclerview as of the time of writing this.
            // Scrolling a couple of items past the currently played item is good enough and easy to do.
            val scrollPosition = min(mainRepository.currentIndex + 2, mainRepository.nowPlayingTracks.size)
            if (mainRepository.nowPlayingTracks.isNotEmpty()) {
                track_rv.scrollToPosition(scrollPosition)
            }
        }
    }

    override fun getExtraActionSheetItems(tracks: List<DbTrack>) = listOfNotNull(
        editPropertiesActionSheetItem(tracks),
        recommendActionSheetItem(tracks),
        addToPlaylistActionSheetItem(tracks),

//        ActionSheetItem("Remove") {
//            // It's in the Trello.
//            // The fact that your currently playing stuff is fragmented across both the MainRepository and PlayerControlsViewModel makes this kind of annoying to do and it's not exactly a super important feature
//        }
    )
}
