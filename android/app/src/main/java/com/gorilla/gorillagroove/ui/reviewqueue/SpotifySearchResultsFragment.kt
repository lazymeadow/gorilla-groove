package com.gorilla.gorillagroove.ui.reviewqueue

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.BackgroundTaskService
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.*
import com.gorilla.gorillagroove.util.GGToast
import kotlinx.android.synthetic.main.fragment_search_spotify_results.*
import kotlinx.android.synthetic.main.track_expandable_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable

class SpotifySearchResultsFragment : GGFragment(R.layout.fragment_search_spotify_results) {

    private lateinit var resultAdapter: ResultAdapter

    private var sources = mapOf<Int, List<MetadataResponseDTO>>()

    // Order of this changes the order that they are shown in
    private var years = emptyList<Int>()

    // We want to use the original artist they searched for when importing to an artist queue, as spotify metadata can have multiple artists on it.
    // Makes it ambiguous as to where the track should be imported.
    private val searchedArtist: String by lazy {
        requireArguments().getString("ARTIST", null)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Spotify Search Results view")

        setupTrackRecyclerView()

        sources = (requireArguments().getSerializable("RESULTS") as MetadataSearchResponse).items.groupBy { it.releaseYear }
        years = sources.keys.sortedDescending()
    }

    override fun onStart() {
        super.onStart()

        resultAdapter.notifyDataSetChanged()
    }

    private fun setupTrackRecyclerView() = resultsList.apply {
        resultAdapter = ResultAdapter()
        adapter = resultAdapter
        addItemDecoration(createDivider(context))
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun showActionSheet(metadata: MetadataResponseDTO) {
        ActionSheet(
            requireActivity(),
            listOf(
                ActionSheetItem("Import to Library") {
                    importTrack(metadata.toImportRequest(null))
                },
                ActionSheetItem("Import to Review Queue") {
                    importTrack(metadata.toImportRequest(searchedArtist))
                }
            )
        )
    }

    private fun importTrack(importRequest: MetadataImportRequest) {
        lifecycleScope.launch(Dispatchers.IO) {
            val response = try {
                Network.api.queueMetadataDownloadTask(importRequest)
            } catch (e: Throwable) {
                logError("Failed to import track!", e)

                GGToast.show("Failed to start import")

                return@launch
            }

            BackgroundTaskService.addBackgroundTasks(response.items)

            GGToast.show("Import started")
        }
    }

    inner class ResultAdapter : HeaderTableAdapter<ResultAdapter.ResultItemViewHolder>() {

        inner class ResultItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateBodyViewHolder(parent: ViewGroup): ResultItemViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.track_expandable_item, parent, false)
            return ResultItemViewHolder(itemView)
        }

        override fun getSectionCount() = sources.keys.size

        override fun getCountForSection(sectionIndex: Int): Int {
            val year = years[sectionIndex]
            return sources.getValue(year).size
        }

        override fun onBindBodyViewHolder(holder: ResultItemViewHolder, sectionIndex: Int, positionInSection: Int) {
            val year = years[sectionIndex]
            val metadata = sources.getValue(year)[positionInSection]

            holder.itemView.setOnClickListener {
                showActionSheet(metadata)
            }

            holder.itemView.checkbox.isVisible = false
            holder.itemView.tv_artist.text = metadata.artist
            holder.itemView.tv_album.text = metadata.album
            holder.itemView.tv_title.text = metadata.name
            holder.itemView.tv_length.text = metadata.length.getSongTimeFromSeconds()
        }

        override fun getTitleForHeader(sectionIndex: Int) = years[sectionIndex].toString()
    }
}

data class MetadataSearchResponse(val items: List<MetadataResponseDTO>) : Serializable
data class MetadataResponseDTO(
    val sourceId: String,
    val name: String,
    val artist: String,
    val album: String,
    val releaseYear: Int,
    val trackNumber: Int,
    val albumArtLink: String?,
    val length: Int,
    val previewUrl: String? // Not all tracks have this. Quite a few don't, actually
) {
    fun toImportRequest(artistQueueName: String?) = MetadataImportRequest(
        name = name,
        artist = artist,
        album = album,
        releaseYear = releaseYear,
        trackNumber = trackNumber,
        albumArtLink = albumArtLink,
        length = length,
        addToReview = artistQueueName != null,
        artistQueueName = artistQueueName
    )
}

data class MetadataImportRequest(
    var name: String,
    val artist: String,
    var album: String,
    val releaseYear: Int,
    val trackNumber: Int,
    val albumArtLink: String?,
    var length: Int,
    val addToReview: Boolean,
    val artistQueueName: String?,
)
