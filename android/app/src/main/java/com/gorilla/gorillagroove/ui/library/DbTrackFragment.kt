package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import com.gorilla.gorillagroove.service.GGSettings
import com.gorilla.gorillagroove.service.TrackService
import com.gorilla.gorillagroove.ui.ActionSheetItem
import com.gorilla.gorillagroove.ui.ActionSheetType
import com.gorilla.gorillagroove.util.GGToast
import com.gorilla.gorillagroove.util.showAlertDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DbTrackFragment : TrackListFragment<DbTrack>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        requireActivity().title_tv.text = "My Library"

        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading My Library view")
    }

    override suspend fun loadTracks(): List<DbTrack> {
        val isHidden = if (showHidden) null else false

        return GorillaDatabase.trackDao.findTracksWithSort(
            sortType = activeSort.sortType,
            isHidden = isHidden,
            albumFilter = albumFilter,
            artistFilter = TrackService.getArtistSqlFilter(artistFilter),
            availableOffline = if (GGSettings.offlineModeEnabled) true else null,
            sortDirection = activeSort.sortDirection
        )
    }

    override fun getExtraActionSheetItems(tracks: List<DbTrack>) = listOfNotNull(
        editPropertiesActionSheetItem(tracks),
        recommendActionSheetItem(tracks),
        addToPlaylistActionSheetItem(tracks),
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
        }
    )
}
