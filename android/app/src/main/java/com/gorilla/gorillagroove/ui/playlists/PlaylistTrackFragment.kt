package com.gorilla.gorillagroove.ui.playlists

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.gorilla.gorillagroove.database.dao.PlaylistTrackDao
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistTrackFragment : TrackListFragment() {

    private lateinit var playlist: DbPlaylist

    @Inject
    lateinit var playlistTrackDao: PlaylistTrackDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playlist = arguments?.getSerializable("PLAYLIST") as? DbPlaylist
            ?: throw IllegalArgumentException("A DbPlaylist with key 'PLAYLIST' must be provided to the PlaylistFragment!")

        requireActivity().title_tv.text = playlist.name
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading PlaylistTracks view with playlist ID: ${playlist.id}")

//        viewModel.setPlaylistsEvent(PlaylistsEvent.GetPlaylist(playlist.id))
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.Default) {
            val tracks = playlistTrackDao.findTracksOnPlaylist(playlist.id)

            withContext(Dispatchers.Main) {
                trackCellAdapter.submitList(tracks)
            }
        }
    }
}
