package com.gorilla.gorillagroove.ui.playlists

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.isVisible
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_track_list.view.*

@AndroidEntryPoint
class PlaylistTrackFragment : TrackListFragment() {

    private lateinit var playlist: DbPlaylist

    init {
        showFilterMenu = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playlist = arguments?.getSerializable("PLAYLIST") as? DbPlaylist
            ?: throw IllegalArgumentException("A DbPlaylist with key 'PLAYLIST' must be provided to the PlaylistFragment!")

        requireActivity().title_tv.text = playlist.name
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading PlaylistTracks view with playlist ID: ${playlist.id}")
    }

    override suspend fun loadTracks(): List<DbTrack> {
        return GorillaDatabase.playlistTrackDao.findTracksOnPlaylist(playlist.id)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        editMenu.isVisible = true

        editMenu.setOnMenuItemClickListener {
            trackCellAdapter.itemTouchHelper.attachToRecyclerView(view?.track_rv)

            toggleReorderState()

            true
        }

        doneMenu.setOnMenuItemClickListener {
            // Passing in "null" when it's not enabled makes the drag functionality not happen via long press / allows us to disable the drag
            trackCellAdapter.itemTouchHelper.attachToRecyclerView(null)

            toggleReorderState()

            true
        }
    }

    private fun toggleReorderState() {
        val reorderEnabled = !trackCellAdapter.reorderEnabled
        trackCellAdapter.reorderEnabled = reorderEnabled
        searchMenu.isVisible = !reorderEnabled

        editMenu.isVisible = !reorderEnabled
        doneMenu.isVisible = reorderEnabled

        requireActivity().multiselectIcon.isVisible = !reorderEnabled
    }

    private fun savePlaylistOrder() {
        val request = ReorderPlaylistRequest(
            playlistId = playlist.id,
            playlistTrackIds =
        )
        Network.api.reorderPlaylist()
    }
}

data class ReorderPlaylistRequest(
    val playlistId: Long,
    val playlistTrackIds: List<Int>,
)
