package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.view.*
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.annotation.MainThread
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.dao.TrackSortType
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.repository.SelectionOperation
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.TrackChangeEvent
import com.gorilla.gorillagroove.service.TrackService
import com.gorilla.gorillagroove.ui.menu.*
import com.gorilla.gorillagroove.util.GGToast
import com.gorilla.gorillagroove.util.LocationService
import com.gorilla.gorillagroove.util.getNullableBoolean
import com.gorilla.gorillagroove.util.showAlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_track_list.*
import kotlinx.android.synthetic.main.track_expandable_item.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject


abstract class TrackListFragment<T: TrackReturnable> : GGFragment(R.layout.fragment_track_list), TrackCellAdapter.OnTrackListener {
    private val playerControlsViewModel: PlayerControlsViewModel by viewModels()
    lateinit var trackCellAdapter: TrackCellAdapter<T>

    @Inject
    lateinit var mainRepository: MainRepository

    protected var activeSort = SortMenuOption("Sort by Name", TrackSortType.NAME, sortDirection = SortDirection.ASC)

    protected var showHidden = false

    protected var showFilterMenu = true

    protected var user: DbUser? = null

    protected lateinit var multiselectOptionsMenu: MenuItem
    private lateinit var filterMenu: MenuItem
    protected lateinit var searchMenu: MenuItem
    private lateinit var playMenu: MenuItem
    protected lateinit var editMenu: MenuItem
    protected lateinit var doneMenu: MenuItem

    private val multiselectEnabled get() = trackCellAdapter.showingCheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getNullableBoolean("SHOW_HIDDEN")?.let { showHidden = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupRecyclerView()
        subscribeObservers()

        if (showFilterMenu) {
            setupFilterMenu()
        }

        (requireActivity() as MainActivity).multiselectIcon.setOnClickListener {
            if (!multiselectEnabled) {
                logInfo("User started multiselect")
                setMultiselect(true)
            } else {
                logInfo("User ended multiselect")
                setMultiselect(false)
            }
        }

        requireActivity().multiselectIcon.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

        reset()
    }

    @MainThread
    private fun reset() {
        loadingIndicator.isVisible = true

        lifecycleScope.launch(Dispatchers.IO) {
            val tracks = loadTracks()
            withContext(Dispatchers.Main) {
                loadingIndicator?.isVisible = false
                trackCellAdapter.submitList(tracks)
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (addToPlaylistView.isVisible) {
            addToPlaylistView.close()
            return true
        } else if (recommendTracksView.isVisible) {
            recommendTracksView.close()
            return true
        }
        return super.onBackPressed()
    }

    abstract suspend fun loadTracks(): List<T>

    private fun setMultiselect(enabled: Boolean) {
        if (multiselectEnabled == enabled) {
            return
        }

        multiselectOptionsMenu.isVisible = enabled
        playMenu.isVisible = enabled
        trackCellAdapter.showingCheckBox = enabled
        searchMenu.isVisible = !enabled

        trackCellAdapter.notifyDataSetChanged()

        if (enabled) {
            filterMenu.isVisible = false
        } else if (showFilterMenu) {
            filterMenu.isVisible = true
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)

        requireActivity().toolbar.apply {
            navigationIcon = null
            setNavigationOnClickListener {}
        }
    }

    private fun setupFilterMenu() {
        popoutMenu.setMenuList(
            listOf(
                *getNavigationOptions(requireView(), LibraryViewType.TRACK, user),
                MenuDivider(),
                SortMenuOption("Sort by Name", TrackSortType.NAME, sortDirection = SortDirection.ASC),
                SortMenuOption("Sort by Play Count", TrackSortType.PLAY_COUNT, initialSortOnTap = SortDirection.DESC),
                SortMenuOption("Sort by Date Added", TrackSortType.DATE_ADDED, initialSortOnTap = SortDirection.DESC),
                SortMenuOption("Sort by Album", TrackSortType.ALBUM),
                SortMenuOption("Sort by Year", TrackSortType.YEAR),
                MenuDivider(),
                CheckedMenuOption(title = "Show Hidden Tracks", false) { showHidden = it.isChecked },
            )
        )

        popoutMenu.onOptionTapped = { menuItem ->
            if (menuItem is SortMenuOption) {
                activeSort = menuItem
            }
            reset()
        }
    }

    private fun setupRecyclerView() = track_rv.apply {
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

        lifecycleScope.launch(Dispatchers.Main) {
            playerControlsViewModel.playbackState.collect {
                trackCellAdapter.isPlaying = it.isPlaying
                trackCellAdapter.notifyDataSetChanged()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onTouchDownEvent(event: MotionEvent) = popoutMenu.handleScreenTap(event, this)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        multiselectOptionsMenu = menu.findItem(R.id.action_multiselect_menu)
        filterMenu = menu.findItem(R.id.action_filter_menu)
        searchMenu = menu.findItem(R.id.action_search)
        editMenu = menu.findItem(R.id.action_edit_menu)
        doneMenu = menu.findItem(R.id.action_done_menu)
        playMenu = menu.findItem(R.id.action_play_now)

        if (!showFilterMenu) {
            filterMenu.isVisible = false
        }

        val searchView = searchMenu.actionView as SearchView
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

        multiselectOptionsMenu.setOnMenuItemClickListener {
            val tracks = trackCellAdapter.getSelectedTracks()
            if (tracks.isEmpty()) {
                lifecycleScope.launch { GGToast.show("Select some tracks first") }
            } else {
                showLibraryActionSheet(tracks.map { it.asTrack() })
            }

            true
        }

        playMenu.setOnMenuItemClickListener {
            playNow(trackCellAdapter.getSelectedTracks().map { it.asTrack() })
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_filter_menu -> {
                popoutMenu.toggleVisibility(ignoreIfRecent = true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onTrackClick(position: Int) {
        val clickedTrack = trackCellAdapter.filteredList[position].asTrack()

        logInfo("User tapped track with ID: ${clickedTrack.id}")

        // Accidental plays when you're trying to reorder are annoying
        if (trackCellAdapter.reorderEnabled) {
            logDebug("User tapped a track but reorder was enabled. Not doing anything.")
            return
        }

        LocationService.requestLocationPermissionIfNeeded(requireActivity())

        playerControlsViewModel.playMedia(position, trackCellAdapter.filteredList.map { it.asTrack() })
    }

    override fun onTrackLongClick(position: Int) {
        if (trackCellAdapter.reorderEnabled) {
            return
        }

        if (!multiselectEnabled) {
            val track = trackCellAdapter.filteredList[position].asTrack()
            showLibraryActionSheet(track)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleTrackChange(event: TrackChangeEvent) {
        if (event.changeType == ChangeType.DELETED) {
            val ids = event.tracks.map { it.id }.toSet()
            logInfo("Removing ${ids.size} track(s) from current track view")

            val newTracks = trackCellAdapter.trackList.filterNot { track -> ids.contains(track.asTrack().id) }

            trackCellAdapter.submitList(newTracks)
        }
    }

    private fun playNow(tracks: List<DbTrack>) {
        mainRepository.setSelectedTracks(tracks, SelectionOperation.PLAY_NOW)
        playerControlsViewModel.playNow(tracks.first())

        setMultiselect(false)
    }

    private fun showLibraryActionSheet(track: DbTrack) = showLibraryActionSheet(listOf(track))

    private fun showLibraryActionSheet(tracks: List<DbTrack>) {
        ActionSheet(
            requireActivity(),
            listOfNotNull(
                ActionSheetItem("Play Now") {
                    playNow(tracks)
                },
                ActionSheetItem("Play Next") {
                    mainRepository.setSelectedTracks(tracks, SelectionOperation.PLAY_NEXT)
                    setMultiselect(false)
                },
                ActionSheetItem("Play Last") {
                    mainRepository.setSelectedTracks(tracks, SelectionOperation.PLAY_LAST)
                    setMultiselect(false)
                },

                // Edit properties is only (currently) available with one track selected. It's a pain to make it work with more. Ticket is in the Trello
                ActionSheetItem("Edit Properties") {
                    findNavController().navigate(
                        R.id.trackPropertiesFragment,
                        bundleOf("KEY_TRACK" to tracks.first()),
                    )
                }.takeIf { tracks.size == 1 },

                ActionSheetItem("Recommend") {
                    lifecycleScope.launch {
                        recommendTracksView.initialize(requireActivity() as MainActivity, tracks)
                    }
                    setMultiselect(false)
                },
                ActionSheetItem("Add to Playlist") {
                    lifecycleScope.launch {
                        addToPlaylistView.initialize(requireActivity() as MainActivity, tracks)
                    }
                    setMultiselect(false)
                },

                ActionSheetItem("Delete", ActionSheetType.DESTRUCTIVE) {
                    showAlertDialog(
                        requireActivity(),
                        message = "Delete " + (if (tracks.size == 1) tracks.first().name else "the selected ${tracks.size} tracks") + "?",
                        yesText = "Delete",
                        noText = "Cancel",
                        yesAction = {
                            lifecycleScope.launch {
                                val success = TrackService.deleteTracks(tracks)
                                if (success) {
                                    // Deletion events are fired that delete the tracks from active views. No need to handle any of that here
                                    GGToast.show("Tracks deleted")
                                } else {
                                    GGToast.show("Failed to delete tracks")
                                }

                                setMultiselect(false)
                            }
                        }
                    )
                },
            )
        )
    }
}

interface TrackReturnable {
    fun asTrack(): DbTrack
}
