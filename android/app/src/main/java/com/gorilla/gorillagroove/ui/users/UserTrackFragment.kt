package com.gorilla.gorillagroove.ui.users

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.database.dao.TrackSortType
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.TrackListFragment
import com.gorilla.gorillagroove.service.sync.EntityPagination
import com.gorilla.gorillagroove.service.sync.TrackResponse
import com.gorilla.gorillagroove.ui.ActionSheetItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
class UserTrackFragment : TrackListFragment<DbTrack>() {

    private var albumFilter: String? = null
    private var artistFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = requireArguments().getSerializable("USER") as DbUser

        requireArguments().getString("ALBUM")?.let { albumFilter ->
            this.albumFilter = albumFilter
        }
        requireArguments().getString("ARTIST")?.let { artistFilter ->
            this.artistFilter = artistFilter
        }

        requireActivity().title_tv.text = "${user!!.name}'s Library"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        val artistFilter = artistFilter
        val albumFilter = albumFilter

        return Network.api.getTracks(
            userId = user!!.id,
            showHidden = showHidden,
            sortString = getApiSort()
        ).content
            // Artists often have multiple people on the same track, so we do a .contains there so that filtering for deadmau5 will find all tracks he collaborated on and not just tracks he was on by himself
            .filter { artistFilter == null || it.artist.contains(artistFilter) || it.featuring.contains(artistFilter) }
            // However, albums we have no reason to do a contains. So here it is an exact match
            .filter { albumFilter == null || it.album == albumFilter }
            .map { it.asTrack() }
    }

    override fun getExtraActionSheetItems(tracks: List<DbTrack>) = listOfNotNull(
        ActionSheetItem("Import") {
            TODO()
        }
    )
}

data class LiveTrackResponse(
    val content: List<TrackResponse>,
    val pageable: EntityPagination,
)
