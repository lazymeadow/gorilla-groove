package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.ui.menu.CheckedMenuOption
import com.gorilla.gorillagroove.ui.menu.LibraryViewType
import com.gorilla.gorillagroove.ui.menu.MenuDivider
import com.gorilla.gorillagroove.ui.menu.getNavigationOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.android.synthetic.main.fragment_artists.popoutMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
class ArtistsFragment : Fragment(R.layout.fragment_artists) {

    lateinit var artistAdapter: ArtistsAdapter

    protected var showHidden = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        logInfo("Loading Artist view")

        setupRecyclerView()

        popoutMenu.setMenuList(
            listOf(
                *getNavigationOptions(requireView(), LibraryViewType.ARTIST),
                MenuDivider(),
                CheckedMenuOption(title = "Show Hidden Tracks", false) {
                    showHidden = it.isChecked
                    loadArtists()
                },
            )
        )

        loadArtists()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun loadArtists() {
        lifecycleScope.launch(Dispatchers.Default) {
            val includeHidden = if (showHidden) null else false
            val artists = GorillaDatabase.trackDao.getDistinctArtists(isHidden = includeHidden, inReview = false)

            withContext(Dispatchers.Main) {
                artistAdapter.submitList(artists)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onTouchDownEvent(event: MotionEvent) = popoutMenu.handleScreenTap(event, this)

    private var searchItem: MenuItem? = null

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem!!.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                artistAdapter.filter.filter(newText)
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

    private fun setupRecyclerView() = artists_rv.apply {
        artistAdapter = ArtistsAdapter { artist ->
            logInfo("Artist '$artist' was tapped")

            searchItem?.collapseActionView()

            val bundle = bundleOf(
                "ARTIST" to artist,
                "SHOW_HIDDEN" to showHidden
            )
            findNavController().navigate(R.id.albumFragment, bundle)
        }
        addItemDecoration(createDivider(context))
        adapter = artistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
}
