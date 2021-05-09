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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
class UserTrackFragment : TrackListFragment() {

    private var albumFilter: String? = null
    private var artistFilter: String? = null
    private val user: DbUser by lazy { requireArguments().getSerializable("USER") as DbUser }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().getString("ALBUM")?.let { albumFilter ->
            this.albumFilter = albumFilter
        }
        requireArguments().getString("ARTIST")?.let { artistFilter ->
            this.artistFilter = artistFilter
        }

        requireActivity().title_tv.text = "${user.name}'s Library"
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
        return Network.api.getTracks(
            userId = user.id,
            showHidden = showHidden,
            sortString = getApiSort()
        ).content.map { it.asTrack() }
    }
}

data class LiveTrackResponse(
    val content: List<TrackResponse>,
    val pageable: EntityPagination,
)
