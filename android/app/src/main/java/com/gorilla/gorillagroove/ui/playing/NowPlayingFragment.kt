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
