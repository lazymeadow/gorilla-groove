package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import com.gorilla.gorillagroove.service.GGSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
class LibraryTrackFragment : TrackListFragment<DbTrack>() {

    private var albumFilter: String? = null
    private var artistFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString("ALBUM")?.let { albumFilter ->
            this.albumFilter = albumFilter
            requireActivity().title_tv.text = albumFilter.takeIf { it.isNotEmpty() } ?: "(No Album)"
        }
        arguments?.getString("ARTIST")?.let { artistFilter ->
            this.artistFilter = artistFilter
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading My Library view")

        requireActivity().title_tv.text = "My Library"
    }

    override suspend fun loadTracks(): List<DbTrack> {
        val isHidden = if (showHidden) null else false
        return GorillaDatabase.trackDao.findTracksWithSort(
            sortType = activeSort.sortType,
            isHidden = isHidden,
            albumFilter = albumFilter,
            artistFilter = artistFilter,
            availableOffline = if (GGSettings.offlineModeEnabled) true else null,
            sortDirection = activeSort.sortDirection
        )
    }
}
