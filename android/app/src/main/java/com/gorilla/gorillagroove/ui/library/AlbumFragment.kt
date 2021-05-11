package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.annotation.MainThread
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.dao.Album
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.ui.menu.CheckedMenuOption
import com.gorilla.gorillagroove.ui.menu.LibraryViewType
import com.gorilla.gorillagroove.ui.menu.MenuDivider
import com.gorilla.gorillagroove.ui.menu.getNavigationOptions
import com.gorilla.gorillagroove.util.getNullableBoolean
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_album.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class AlbumFragment : Fragment(R.layout.fragment_album) {

    private lateinit var albumAdapter: AlbumAdapter

    protected var showHidden = false

    protected var artistFilter: String? = null

    protected var user: DbUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        artistFilter = arguments?.getString("ARTIST")
        arguments?.getNullableBoolean("SHOW_HIDDEN")?.let { showHidden = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        logInfo("Loading Album view")

        setupRecyclerView()

        popoutMenu.setMenuList(
            listOf(
                *getNavigationOptions(requireView(), LibraryViewType.ALBUM, user),
                MenuDivider(),
                CheckedMenuOption(title = "Show Hidden Tracks", showHidden) {
                    showHidden = it.isChecked
                    loadAlbums()
                },
            )
        )

        loadAlbums()

        artistFilter?.let { artistFilter ->
            requireActivity().title_tv.text = artistFilter.takeIf { it.isNotEmpty() } ?: "(No Artist)"
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    abstract suspend fun getAlbums(): List<Album>

    @MainThread
    private fun loadAlbums() {
        loadingIndicator.isVisible = true
        lifecycleScope.launch(Dispatchers.Default) {
            val albums = getAlbums().toMutableList()

            if (albums.size > 1) {
                albums.add(0, Album("View All", VIEW_ALL))
            }

            withContext(Dispatchers.Main) {
                loadingIndicator?.isVisible = false
                albumAdapter.submitList(albums)
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
                albumAdapter.filter.filter(newText)
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

    private fun setupRecyclerView() = album_rv.apply {
        albumAdapter = AlbumAdapter { album ->
            logInfo("Album '$album' was tapped")

            searchItem?.collapseActionView()

            val bundle = bundleOf(
                "ALBUM" to if (album.trackId == VIEW_ALL) null else album.name,
                "ARTIST" to artistFilter,
                "SHOW_HIDDEN" to showHidden,
                "USER" to user,
            )

            val id = if (user == null) R.id.libraryTrackFragment else R.id.userTrackFragment
            findNavController().navigate(id, bundle)
        }
        addItemDecoration(createDivider(context))
        adapter = albumAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
}

const val VIEW_ALL = 0L
