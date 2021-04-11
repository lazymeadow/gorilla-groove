package com.gorilla.gorillagroove.ui

import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.repository.SelectionOperation
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.menu.*
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
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var mainRepository: MainRepository

    private lateinit var optionsMenu: Menu

    protected var sortType = TrackSortType.NAME
        private set
    protected var sortDirection = SortDirection.ASC
        private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupRecyclerView()
        subscribeObservers()

        popoutMenu.setMenuList(
            listOf(
                CheckedMenuOption(title = "View by Track", true) {
                    logInfo("Hey I was clicked 1")
                },
                CheckedMenuOption(title = "View by Artist", false) {
                    logInfo("Hey I was clicked 1")
                },
                CheckedMenuOption(title = "View by Album", false) {
                    logInfo("Hey I was clicked 1")
                },
                MenuDivider(),
                SortMenuOption(title = "Sort by Name", sortDirection = SortDirection.ASC) {
                    logInfo("Hey I was clicked 2")
                },
                SortMenuOption(title = "Sort by Play Count", initialSortOnTap = SortDirection.DESC) {
                    logInfo("Hey I was clicked 2")
                },
                SortMenuOption(title = "Sort by Date Added", initialSortOnTap = SortDirection.DESC) {
                    logInfo("Hey I was clicked 2")
                },
                SortMenuOption(title = "Sort by Album") {
                    logInfo("Hey I was clicked 2")
                },
                SortMenuOption(title = "Sort by Year") {
                    logInfo("Hey I was clicked 2")
                },
                MenuDivider(),
                CheckedMenuOption(title = "Show Hidden Tracks", false) {
                    logInfo("Hey I was clicked 1")
                },
            )
        )
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
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
    fun onTouchDownEvent(event: MotionEvent) {
        if (popoutMenu.visibility == View.GONE) {
            return
        }

        // If someone touched the menu, don't manage the event as the menu knows how to manage its own visibility
        if (popoutMenu.containsMotionEvent(requireView(), event)) {
            return
        }

        // If we got this far then we touched something that wasn't the popout menu or the toolbar and we can close it right away
        popoutMenu.toggleVisibility()
    }

    private fun View.containsMotionEvent(parentView: View, event: MotionEvent): Boolean {
        val bounds = Rect().apply { getHitRect(this) }
        val locationOnScreen = IntArray(2)

        requireActivity().activity_main_root.getLocationOnScreen(locationOnScreen)
        val (activityOffsetWidth, activityOffsetHeight) = locationOnScreen

        parentView.getLocationOnScreen(locationOnScreen)
        val (viewOffsetWidth, viewOffsetHeight) = locationOnScreen

        val effectiveWidth = event.x.toInt() + activityOffsetWidth - viewOffsetWidth
        val effectiveHeight = event.y.toInt() + activityOffsetHeight - viewOffsetHeight

        return (bounds.contains(effectiveWidth, effectiveHeight))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        optionsMenu = menu

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
        val bundle = bundleOf("KEY_TRACK" to track)

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

enum class TrackSortType {
    NAME, PLAY_COUNT, DATE_ADDED, ALBUM, YEAR
}
