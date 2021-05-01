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
import com.gorilla.gorillagroove.database.dao.TrackDao
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.network.NetworkApi
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.ui.menu.CheckedMenuOption
import com.gorilla.gorillagroove.ui.menu.LibraryViewType
import com.gorilla.gorillagroove.ui.menu.MenuDivider
import com.gorilla.gorillagroove.ui.menu.getNavigationOptions
import com.gorilla.gorillagroove.util.getNullableBoolean
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_album.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@AndroidEntryPoint
class AlbumFragment : Fragment(R.layout.fragment_album) {

    lateinit var albumAdapter: AlbumAdapter

    private var showHidden = false

    private var artistFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString("ARTIST")?.let { artistFilter ->
            this.artistFilter = artistFilter
            requireActivity().title_tv.text = artistFilter
        }

        arguments?.getNullableBoolean("SHOW_HIDDEN")?.let { showHidden = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        logInfo("Loading Album view")

        setupRecyclerView()

        popoutMenu.setMenuList(
            listOf(
                *getNavigationOptions(requireView(), LibraryViewType.ALBUM),
                MenuDivider(),
                CheckedMenuOption(title = "Show Hidden Tracks", showHidden) {
                    showHidden = it.isChecked
                    loadAlbums()
                },
            )
        )

        loadAlbums()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun loadAlbums() {
        lifecycleScope.launch(Dispatchers.Default) {
            val includeHidden = if (showHidden) null else false
            val albums = GorillaDatabase.trackDao.getDistinctAlbums(artistFilter = artistFilter, isHidden = includeHidden, inReview = false)

            withContext(Dispatchers.Main) {
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
                "ALBUM" to album.name,
                "ARTIST" to artistFilter,
                "SHOW_HIDDEN" to showHidden,
            )
            findNavController().navigate(R.id.libraryTrackFragment, bundle)
        }
        addItemDecoration(createDivider(context))
        adapter = albumAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
}
