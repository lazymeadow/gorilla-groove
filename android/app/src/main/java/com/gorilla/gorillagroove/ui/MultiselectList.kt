package com.gorilla.gorillagroove.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.sync.PlaylistTrackResponse
import com.gorilla.gorillagroove.util.GGToast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.multiselect_list.view.*
import kotlinx.android.synthetic.main.multiselect_list_item.view.*
import kotlinx.coroutines.*
import java.util.*

open class MultiselectList(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    val layout = LayoutInflater.from(context).inflate(R.layout.multiselect_list, this, true) as ConstraintLayout
}

class AddToPlaylistView(context: Context, attrs: AttributeSet? = null) : MultiselectList(context, attrs) {
    private var playlists = listOf<DbPlaylist>()

    lateinit var multiselectAdapter: MultiselectListAdapter

    private lateinit var mainActivity: MainActivity
    private lateinit var tracksToAdd: List<Long>

    val checkedPlaylistIds = mutableSetOf<Long>()

    suspend fun initialize(activity: MainActivity, tracks: List<DbTrack>) = withContext(Dispatchers.Main) {
        withContext(Dispatchers.IO) {
            playlists = GorillaDatabase.playlistDao.findAll().sortedBy { it.name.toLowerCase(Locale.getDefault()) }
        }

        if (!::multiselectAdapter.isInitialized) {
            setupRecyclerView()
        }

        multiselectAdapter.notifyDataSetChanged()

        mainActivity = activity
        tracksToAdd = tracks.map { it.id }

        activity.toolbar.isVisible = false
        rightLoadingIndicator.isVisible = false

        selectionTitle.text = "Add to Playlist"
        addButton.isVisible = true

        addButton.setOnClickListener {
            addPlaylists()
        }

        layout.visibility = View.VISIBLE
    }

    @MainThread
    fun close() {
        playlists = emptyList()
        checkedPlaylistIds.clear()

        mainActivity.toolbar.isVisible = true
        layout.visibility = View.GONE
    }

    private fun addPlaylists() {
        logInfo("User tapped 'Add' button")

        if (checkedPlaylistIds.isEmpty()) {
            logInfo("No playlists were chosen")
            return
        }

        val request = AddToPlaylistRequest(playlistIds = checkedPlaylistIds.toList(), trackIds = tracksToAdd)

        val plurality = if (checkedPlaylistIds.size == 1) "playlist" else "playlists"

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

    private fun setupRecyclerView() = multiselectList.apply {
        multiselectAdapter = MultiselectListAdapter()
        addItemDecoration(createDivider(context))
        adapter = MultiselectListAdapter()
        layoutManager = LinearLayoutManager(context)
    }

    inner class MultiselectListAdapter : RecyclerView.Adapter<MultiselectListAdapter.MultiselectItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiselectItemViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.multiselect_list_item, parent, false)
            return MultiselectItemViewHolder(itemView)
        }

        override fun getItemCount() = playlists.size

        override fun onBindViewHolder(holder: MultiselectItemViewHolder, position: Int) {
            val playlist = playlists[position]
            holder.setPlaylist(playlist)
        }

        inner class MultiselectItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private lateinit var playlist: DbPlaylist

            init {
                itemView.setOnClickListener {
                    if (checkedPlaylistIds.contains(playlist.id)) {
                        checkedPlaylistIds.remove(playlist.id)
                    } else {
                        checkedPlaylistIds.add(playlist.id)
                    }

                    itemView.itemCheckmark.visibility = if (checkedPlaylistIds.contains(playlist.id)) View.VISIBLE else View.INVISIBLE
                }
            }

            fun setPlaylist(playlist: DbPlaylist) {
                this.playlist = playlist

                itemView.itemTitle.text = playlist.name
                itemView.itemCheckmark.visibility = if (checkedPlaylistIds.contains(playlist.id)) View.VISIBLE else View.INVISIBLE
            }
        }
    }
}

data class AddToPlaylistRequest(
    val trackIds: List<Long>,
    val playlistIds: List<Long>,
)

data class AddToPlaylistResponse(val items: List<PlaylistTrackResponse>)
