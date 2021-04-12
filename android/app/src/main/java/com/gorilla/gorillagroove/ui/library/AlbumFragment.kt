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
import com.gorilla.gorillagroove.database.dao.TrackDao
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.ui.menu.CheckedMenuOption
import com.gorilla.gorillagroove.ui.menu.LibraryViewType
import com.gorilla.gorillagroove.ui.menu.MenuDivider
import com.gorilla.gorillagroove.ui.menu.getNavigationOptions
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

    @Inject
    lateinit var trackDao: TrackDao

    protected var showHidden = false

    private var artistFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString("ARTIST")?.let { artistFilter ->
            this.artistFilter = artistFilter
            requireActivity().title_tv.text = artistFilter
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        logInfo("Loading Users view")

        setupRecyclerView()

        popoutMenu.setMenuList(
            listOf(
                *getNavigationOptions(requireView(), LibraryViewType.ALBUM),
                MenuDivider(),
                CheckedMenuOption(title = "Show Hidden Tracks", false) { showHidden = it.isChecked },
            )
        )

        popoutMenu.onOptionTapped = {
        }

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
            val albums = trackDao.getDistinctAlbums(artistFilter = artistFilter)

            withContext(Dispatchers.Main) {
                albumAdapter.submitList(albums)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onTouchDownEvent(event: MotionEvent) = popoutMenu.handleScreenTap(event, this)

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

            val bundle = bundleOf("ALBUM" to album)
            findNavController().navigate(R.id.libraryTrackFragment, bundle)
        }
        addItemDecoration(createDivider(context))
        adapter = albumAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
}
