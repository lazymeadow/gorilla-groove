package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.view.*
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.dao.TrackSortType
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.repository.SelectionOperation
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.menu.*
import com.gorilla.gorillagroove.util.LocationService
import com.gorilla.gorillagroove.util.getNullableBoolean
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_track_list.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject


open class TrackListFragment : Fragment(R.layout.fragment_track_list), TrackCellAdapter.OnTrackListener {
    protected val playerControlsViewModel: PlayerControlsViewModel by viewModels()
    lateinit var trackCellAdapter: TrackCellAdapter
    var actionMode: ActionMode? = null

    @Inject
    lateinit var mainRepository: MainRepository

    protected var activeSort = SortMenuOption("Sort by Name", TrackSortType.NAME, sortDirection = SortDirection.ASC)

    protected var showHidden = false

    protected var showFilterMenu = true

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
    }

    open fun onFiltersChanged() {}

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

        (requireActivity() as MainActivity).multiselectIcon.setOnClickListener {
            if (actionMode == null) {
                logInfo("User started multiselect")
                trackCellAdapter.showingCheckBox = true
                trackCellAdapter.notifyDataSetChanged()
                actionMode = activity?.startActionMode(actionModeCallback)!!
            } else {
                logInfo("User ended multiselect")
                trackCellAdapter.showingCheckBox = false
                trackCellAdapter.notifyDataSetChanged()
                actionMode?.finish()
            }
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
                *getNavigationOptions(requireView(), LibraryViewType.TRACK),
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
            onFiltersChanged()
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
        playerControlsViewModel.playbackState.observe(requireActivity(), {
            trackCellAdapter.isPlaying = it.isPlaying
            trackCellAdapter.notifyDataSetChanged()
        })
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onTouchDownEvent(event: MotionEvent) = popoutMenu.handleScreenTap(event, this)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        if (!showFilterMenu) {
            menu.findItem(R.id.action_filter_menu).isVisible = false
        }

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
            R.id.action_filter_menu -> {
                popoutMenu.toggleVisibility(ignoreIfRecent = true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onTrackClick(position: Int) {
        val clickedTrack = trackCellAdapter.filteredList[position]

        logInfo("User tapped track with ID: ${clickedTrack.id}")

        LocationService.requestLocationPermissionIfNeeded(requireActivity())

        playerControlsViewModel.playMedia(position, trackCellAdapter.filteredList)
    }

    override fun onTrackLongClick(position: Int) {
        when (actionMode) {
            null -> {
                val track = trackCellAdapter.filteredList[position]
                showLibraryActionSheet(track)
            }
            else -> {
            }
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
            val inflater: MenuInflater = mode?.menuInflater ?: return false

            mode.title = "Selecting tracks..."
            inflater.inflate(R.menu.context_action_menu, menu)

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
                R.id.action_play_now_button, R.id.action_play_now -> {
                    val selectedTracks = trackCellAdapter.getSelectedTracks()
                    mainRepository.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NOW)
                    playerControlsViewModel.playNow(selectedTracks[0])
                    mode?.finish()
                    true
                }
                R.id.action_play_next -> {
                    val selectedTracks = trackCellAdapter.getSelectedTracks()
                    mainRepository.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NEXT)
                    mode?.finish()
                    true
                }
                R.id.action_play_last -> {
                    val selectedTracks = trackCellAdapter.getSelectedTracks()
                    mainRepository.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_LAST)
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
