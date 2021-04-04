package com.gorilla.gorillagroove.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.repository.SelectionOperation
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.Constants
import kotlinx.android.synthetic.main.fragment_main.*
import javax.inject.Inject

open class TrackListFragment : Fragment(R.layout.fragment_main), TrackCellAdapter.OnTrackListener {
    protected val viewModel: MainViewModel by viewModels()
    protected val playerControlsViewModel: PlayerControlsViewModel by viewModels()
    lateinit var trackCellAdapter: TrackCellAdapter
    var actionMode: ActionMode? = null

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupRecyclerView()
        subscribeObservers()
    }

    private fun setupRecyclerView() = playlist_rv.apply {
        trackCellAdapter = TrackCellAdapter(this@TrackListFragment)
        addItemDecoration(createDivider(context))
        adapter = trackCellAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        playerControlsViewModel.currentTrackItem.observe(requireActivity(), {
            val mediaId = it.description.mediaId.toString()
            if (mediaId != "") {
                trackCellAdapter.playingTrackId = mediaId
                trackCellAdapter.notifyDataSetChanged()
            }
        })
        playerControlsViewModel.playbackState.observe(requireActivity(), {
            trackCellAdapter.isPlaying = it.isPlaying
            trackCellAdapter.notifyDataSetChanged()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                trackCellAdapter.filter.filter(newText)
                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_sort_az -> {
                sharedPref.edit()
                    .putString(Constants.KEY_SORT, Constants.SORT_BY_AZ)
                    .apply()
                true
            }
            R.id.action_sort_id -> {
                sharedPref.edit()
                    .putString(Constants.KEY_SORT, Constants.SORT_BY_ID)
                    .apply()
                true
            }
            R.id.action_sort_date_added_oldest -> {
                sharedPref.edit()
                    .putString(Constants.KEY_SORT, Constants.SORT_BY_DATE_ADDED_OLDEST)
                    .apply()
                true
            }
            R.id.action_sort_date_added_newest -> {
                sharedPref.edit()
                    .putString(Constants.KEY_SORT, Constants.SORT_BY_DATE_ADDED_NEWEST)
                    .apply()
                true
            }
            R.id.action_sort_artist_az -> {
                sharedPref.edit()
                    .putString(Constants.KEY_SORT, Constants.SORT_BY_ARTIST_AZ)
                    .apply()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onTrackClick(position: Int) {
        val clickedTrack = trackCellAdapter.filteredList[position]

        logInfo("User tapped track with ID: ${clickedTrack.id}")

        playerControlsViewModel.playMedia(position, trackCellAdapter.filteredList)
    }

    override fun onTrackLongClick(position: Int): Boolean {
        //Log.d(TAG, "onTrackLongClick: Long clicked $position")
        return when (actionMode) {
            null -> {
                trackCellAdapter.showingCheckBox = true
                trackCellAdapter.notifyDataSetChanged()
                actionMode = activity?.startActionMode(actionModeCallback)!!
                view?.isSelected = true
                true
            }
            else -> {

                false
            }
        }
    }

    override fun onPlayPauseClick(position: Int) {
        trackCellAdapter.isPlaying = playerControlsViewModel.playPause()
        trackCellAdapter.notifyDataSetChanged()
    }

    override fun onOptionsClick(position: Int) {
        //Log.d(TAG, "onOptionsClick: ")
    }

    override fun onPlayNextSelection(position: Int) {
        //Log.d(TAG, "onPlayNextSelection: ")
    }

    override fun onPlayLastSelection(position: Int) {
        //Log.d(TAG, "onPlayLastSelection: ")
    }

    override fun onGetLinkSelection(position: Int) {
        //Log.d(TAG, "onGetLinkSelection: ")
    }

    override fun onDownloadSelection(position: Int) {
        //Log.d(TAG, "onDownloadSelection: ")
    }

    override fun onRecommendSelection(position: Int) {
        //Log.d(TAG, "onRecommendSelection: ")
    }

    override fun onAddToPlaylistSelection(position: Int) {
        //Log.d(TAG, "onAddToPlaylistSelection: ")
    }

    override fun onPropertiesSelection(position: Int) {
        val track = trackCellAdapter.filteredList[position]
        val bundle = bundleOf("KEY_TRACK_ID" to track.id)

        findNavController().navigate(
            R.id.trackPropertiesFragment,
            bundle
        )
    }


    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
            val inflater: MenuInflater? = mode?.menuInflater
            mode?.title = "Selecting tracks..."
            inflater?.inflate(R.menu.context_action_menu, menu)

            //required because the XML is overridden
            menu[0].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu[1].setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu[2].setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu[3].setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_play_now_button -> {
                    val selectedTracks = trackCellAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NOW)
                    trackCellAdapter.trackList.find { track -> track.id == selectedTracks[0] }?.let {
                        playerControlsViewModel.playNow(it)
                    }
                    mode?.finish()
                    true
                }
                R.id.action_play_now -> {
                    val selectedTracks = trackCellAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NOW)
                    trackCellAdapter.trackList.find { track -> track.id == selectedTracks[0] }?.let {
                        playerControlsViewModel.playNow(it)
                    }
                    mode?.finish()
                    true
                }
                R.id.action_play_next -> {
                    val selectedTracks = trackCellAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NEXT)
                    mode?.finish()
                    true
                }
                R.id.action_play_last -> {
                    val selectedTracks = trackCellAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_LAST)
                    mode?.finish()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            trackCellAdapter.showingCheckBox = false
            trackCellAdapter.checkedTracks.clear()
            trackCellAdapter.notifyDataSetChanged()
            actionMode = null
        }
    }
}
