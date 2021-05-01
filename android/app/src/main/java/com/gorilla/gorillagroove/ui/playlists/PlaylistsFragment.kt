package com.gorilla.gorillagroove.ui.playlists


import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.dao.PlaylistDao
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.createDivider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_playlists.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistsFragment : Fragment(R.layout.fragment_playlists), PlaylistAdapter.OnPlaylistListener {

    private lateinit var playlistAdapter: PlaylistAdapter
    private var savedInstanceStateBundle: Bundle? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Playlists view")

        setHasOptionsMenu(true)
        savedInstanceStateBundle = savedInstanceState
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.Default) {
            val playlists = GorillaDatabase.playlistDao.findAll()

            withContext(Dispatchers.Main) {
                playlistAdapter.setPlaylists(playlists)
            }
        }
    }

    private fun setupRecyclerView() = playlists_key_rv.apply {
        playlistAdapter = PlaylistAdapter(this@PlaylistsFragment)
        addItemDecoration(createDivider(context))
        adapter = playlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onPlaylistClick(position: Int) {
        val playlist = playlistAdapter.playlists[position]
        val bundle = bundleOf("PLAYLIST" to playlist)

        findNavController().navigate(
            R.id.playlistTrackFragment,
            bundle
        )
    }

    override fun onPlaylistLongClick(position: Int): Boolean {
        return true
    }
}
