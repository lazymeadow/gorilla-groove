package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.gorilla.gorillagroove.database.dao.TrackDao
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
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

        loadTracks()
    }

    private fun loadTracks() {
        lifecycleScope.launch(Dispatchers.Default) {
            val tracks = trackDao.findAll()

            withContext(Dispatchers.Main) {
                trackCellAdapter.submitList(tracks)
            }
        }
    }
}
