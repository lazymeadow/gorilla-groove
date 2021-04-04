package com.gorilla.gorillagroove.ui.playlists


import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.MainViewModel
import com.gorilla.gorillagroove.ui.createDivider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_playlists.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class PlaylistsFragment : Fragment(R.layout.fragment_playlists), PlaylistAdapter.OnPlaylistListener {

    private val viewModel: MainViewModel by viewModels()
    lateinit var playlistKeyAdapter: PlaylistAdapter
    private var savedInstanceStateBundle: Bundle? = null

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Playlists view")

        setHasOptionsMenu(true)
        savedInstanceStateBundle = savedInstanceState
        setupRecyclerView()
    }

    private fun setupRecyclerView() = playlists_key_rv.apply {
        playlistKeyAdapter = PlaylistAdapter(this@PlaylistsFragment)
        addItemDecoration(createDivider(context))
        adapter = playlistKeyAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onPlaylistClick(position: Int) {
        val playlist = playlistKeyAdapter.playlists[position]
        val bundle = bundleOf("PLAYLIST" to playlist)

        findNavController().navigate(
            R.id.action_playlistsFragment_to_playlistFragment,
            bundle
        )
    }

    override fun onPlaylistLongClick(position: Int): Boolean {
        //Log.d(TAG, "onPlaylistLongClick: Long Clicked: $position")

        return true
    }
}
