package com.gorilla.gorillagroove.ui.multiselectlist

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.sync.PlaylistTrackResponse
import com.gorilla.gorillagroove.ui.MainActivity
import com.gorilla.gorillagroove.util.GGToast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.multiselect_list.view.*
import kotlinx.android.synthetic.main.multiselect_list_item.view.*
import kotlinx.coroutines.*
import java.util.*


class PlaylistItemViewHolder(itemView: View, checkedIds: MutableSet<Long>) : MultiselectList.MultiselectItemViewHolder<DbPlaylist>(itemView, checkedIds) {
    override fun getRowName(datum: DbPlaylist) = datum.name
}

class AddToPlaylistView(context: Context, attrs: AttributeSet? = null) : MultiselectList<DbPlaylist>(context, attrs) {
    override fun loadData(): List<DbPlaylist> {
        return GorillaDatabase.playlistDao.findAll().sortedBy { it.name.toLowerCase(Locale.getDefault()) }
    }

    override fun createMyViewHolder(itemView: View, checkedIds: MutableSet<Long>): MultiselectItemViewHolder<DbPlaylist> {
        return PlaylistItemViewHolder(itemView, checkedIds)
    }

    override suspend fun initialize(activity: MainActivity, tracks: List<DbTrack>) = withContext(Dispatchers.Main) {
        super.initialize(activity, tracks)

        addButton.setOnClickListener {
            addPlaylists()
        }

        selectionTitle.text = "Add to Playlist"
    }

    private fun addPlaylists() {
        logInfo("User tapped 'Add' button")

        if (checkedDataIds.isEmpty()) {
            logInfo("No playlists were chosen")
            return
        }

        val request = AddToPlaylistRequest(playlistIds = checkedDataIds.toList(), trackIds = tracksToAdd)

        val plurality = if (checkedDataIds.size == 1) "playlist" else "playlists"

        addButton.isVisible = false
        rightLoadingIndicator.isVisible = true

        GlobalScope.launch(Dispatchers.IO) {
            logInfo("Adding tracks to playlists: $request")

            val response = try {
                Network.api.addTracksToPlaylists(request)
            } catch (e: Throwable) {
                logError("Failed to add to $plurality!", e)

                GGToast.show("Failed to add to $plurality")

                withContext(Dispatchers.Main) {
                    addButton.isVisible = true
                    rightLoadingIndicator.isVisible = false
                }

                return@launch
            }

            logInfo("Tracks were added on the API. Adding local records")

            val playlistTracks = response.items.map { it.asPlaylistTrack() }

            GorillaDatabase.playlistTrackDao.save(playlistTracks)

            GGToast.show("Successfully added to $plurality")

            withContext(Dispatchers.Main) {
                close()
            }
        }
    }

}

data class AddToPlaylistRequest(
    val trackIds: List<Long>,
    val playlistIds: List<Long>,
)

data class AddToPlaylistResponse(val items: List<PlaylistTrackResponse>)
