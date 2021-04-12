package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.gorilla.gorillagroove.database.dao.TrackDao
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import com.gorilla.gorillagroove.ui.menu.SortMenuOption
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class LibraryFragment : TrackListFragment() {

    @Inject
    lateinit var trackDao: TrackDao

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading My Library view")

        // TODO every time this fragment is started, the tracks are reloaded. Can't simply check if tracks already exist because they are lost.
        // Might need to save tracks in a view model so they aren't. This isn't a big deal, but it is most annoying when you edit the properties
        // of a track, and then you go back a screen, and you lost your scroll position. Need to handle this higher up than just the LibraryFragment
        // tho since it does affect every track screen with a menu.
        loadTracks()
    }

    override fun onFiltersChanged() {
        super.onFiltersChanged()

        loadTracks()
    }

    private fun loadTracks() {
        lifecycleScope.launch(Dispatchers.Default) {
            val isHidden = if (showHidden) null else false
            val tracks = trackDao.findTracksWithSort(sortType = activeSort.sortType, isHidden = isHidden, sortDirection = activeSort.sortDirection)

            withContext(Dispatchers.Main) {
                trackCellAdapter.submitList(tracks)
            }
        }
    }
}
