package com.gorilla.gorillagroove.ui.reviewqueue

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.*
import kotlinx.android.synthetic.main.fragment_search_spotify_results.*
import kotlinx.android.synthetic.main.track_expandable_item.view.*
import java.io.Serializable

class SpotifySearchResultsFragment : GGFragment(R.layout.fragment_search_spotify_results) {

    private lateinit var resultAdapter: ResultAdapter

    private var sources = mapOf<Int, List<MetadataResponseDTO>>()

    // Order of this changes the order that they are shown in
    private var years = emptyList<Int>()

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

    private fun showAddActionSheet(metadata: MetadataResponseDTO) {
        ActionSheet(
            requireActivity(),
            listOf(
                ActionSheetItem("Import to Library") {

                },
                ActionSheetItem("Import to Review Queue") {

                }
            )
        )
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

            holder.itemView.setOnLongClickListener {
//                showEditActionSheet(source)
                true
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
)
