package com.gorilla.gorillagroove.ui.users

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.dao.TrackSortType
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.TrackService
import com.gorilla.gorillagroove.ui.TrackListFragment
import com.gorilla.gorillagroove.service.sync.EntityPagination
import com.gorilla.gorillagroove.service.sync.TrackResponse
import com.gorilla.gorillagroove.ui.ActionSheetItem
import com.gorilla.gorillagroove.util.GGToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class UserTrackFragment : TrackListFragment<DbTrack>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        user = requireArguments().getSerializable("USER") as DbUser

        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title_tv.text = "${user!!.name}'s Library"

        logInfo("Loading User Track view")
    }

    private fun getApiSort(): List<String> {
        val sorts = mutableListOf("${activeSort.sortType.apiPropertyName},${activeSort.sortDirection}")

        if (activeSort.sortType == TrackSortType.ALBUM) {
            sorts.add("trackNumber,ASC")
        } else if (activeSort.sortType == TrackSortType.YEAR) {
            sorts.add("album,ASC")
            sorts.add("trackNumber,ASC")
        }

        return sorts
    }

    override suspend fun loadTracks(): List<DbTrack> {
        val albumFilter = albumFilter

        return Network.api.getTracks(
            userId = user!!.id,
            showHidden = showHidden,
            sortString = getApiSort()
        ).content
            .filter { TrackService.passesArtistFilter(artistFilter, it) }
            .filter { albumFilter == null || it.album == albumFilter }
            .map { it.asTrack() }
    }

    override fun getExtraActionSheetItems(tracks: List<DbTrack>) = listOfNotNull(
        ActionSheetItem("Import") {
            lifecycleScope.launch(Dispatchers.IO) {
                val trackIds = tracks.map { it.id }

                logInfo("Importing tracks: $trackIds")

                // Apparently you can't show a toast for a given duration. Thanks, Google. I'd rather show this for an indefinite duration but this is probably fine MOST of the time
                GGToast.show("Importing ...", Toast.LENGTH_LONG)

                val importResponse = try {
                    Network.api.importUserTrack(ImportTrackRequest(trackIds))
                } catch (e: Throwable) {
                    logError("Failed to import tracks!", e)
                    GGToast.show("Failed to import")

                    return@launch
                }

                val newTracks = importResponse.items.map { it.asTrack() }
                GorillaDatabase.trackDao.save(newTracks)

                GGToast.show("Tracks imported")

                logInfo("New imports saved")

                withContext(Dispatchers.Main) {
                    setMultiselect(false)
                }
            }
        }
    )
}

data class LiveTrackResponse(
    val content: List<TrackResponse>,
    val pageable: EntityPagination,
)

data class ImportTrackRequest(val trackIds: List<Long>)
data class ImportTrackResponse(val items: List<TrackResponse>)
