package com.gorilla.gorillagroove.ui.reviewqueue

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbReviewSource
import com.gorilla.gorillagroove.database.entity.ReviewSourceType
import com.gorilla.gorillagroove.database.entity.ReviewSourceType.*
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.ActionSheet
import com.gorilla.gorillagroove.ui.ActionSheetItem
import com.gorilla.gorillagroove.ui.GGFragment
import com.gorilla.gorillagroove.ui.createDivider
import kotlinx.android.synthetic.main.fragment_edit_review_sources.*
import kotlinx.android.synthetic.main.simple_text_info_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TYPE_HEADER = 0
private const val TYPE_ITEM = 1

class EditReviewSourcesFragment : GGFragment(R.layout.fragment_edit_review_sources) {

    private val reviewSourceDao get() = GorillaDatabase.getDatabase().reviewSourceDao()

    private lateinit var sourceListAdapter: SourceListAdapter

    private var sources = listOf<DbReviewSource>()
    private var sourceTypeCount = mapOf<ReviewSourceType, Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Edit Review Sources view")

        setupTrackRecyclerView()

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.IO) {
            val groups = reviewSourceDao.getEditableSources().groupBy { it.sourceType }.toMutableMap()

            // Order of this changes the order that they are shown in
            val typesToShow = listOf(ARTIST, YOUTUBE_CHANNEL)

            typesToShow.forEach { type ->
                if (groups[type] == null) {
                    groups[type] = emptyList()
                }
            }

            sources = typesToShow.fold(emptyList()) { acc, type -> acc + groups.getOrDefault(type, emptyList()) }
            sourceTypeCount = groups.mapValues { it.value.size }
            sourceListAdapter.notifyDataSetChanged()
        }
    }

    private fun setupTrackRecyclerView() = sourcesList.apply {
        sourceListAdapter = SourceListAdapter()
        adapter = sourceListAdapter
        addItemDecoration(createDivider(context))
        layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.add_bar_menu, menu)
        menu.findItem(R.id.action_add_menu).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_add_menu -> {
                showAddActionSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddActionSheet() {
        ActionSheet(
            requireActivity(),
            listOf(
                ActionSheetItem("Artist") {
                    findNavController().navigate(
                        R.id.addReviewSourceFragment,
                        bundleOf("MODE" to AddSourceMode.SPOTIFY_ARTIST),
                    )
                },
                ActionSheetItem("YouTube Channel") {
                    findNavController().navigate(
                        R.id.addReviewSourceFragment,
                        bundleOf("MODE" to AddSourceMode.YOUTUBE_CHANNEL),
                    )
                }
            )
        )
    }

    inner class SourceListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_ITEM) {
                val itemView = inflater.inflate(R.layout.simple_text_info_item, parent, false)
                ReviewQueueItemViewHolder(itemView)
            } else {
                val itemView = inflater.inflate(R.layout.simple_text_header_item, parent, false)
                ReviewQueueHeaderViewHolder(itemView)
            }
        }

        override fun getItemCount(): Int {
            val (artistHeaderPosition, youtubeHeaderPosition) = getSectionHeaderPositions()
            var visibleHeaderCount = 0
            if (artistHeaderPosition > -1) visibleHeaderCount++
            if (youtubeHeaderPosition > -1) visibleHeaderCount++

            return sources.size + visibleHeaderCount
        }

        private fun getSectionHeaderPositions(): Pair<Int, Int> {
            val artistHeaderPosition = if (sourceTypeCount.getValue(ARTIST) > 0) 0 else -1
            val youtubeHeaderPosition = if (sourceTypeCount.getValue(YOUTUBE_CHANNEL) > 0) {
                artistHeaderPosition + sourceTypeCount.getValue(ARTIST) + 1
            } else {
                -1
            }

            return artistHeaderPosition to youtubeHeaderPosition
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val (artistHeaderPosition, youtubeHeaderPosition) = getSectionHeaderPositions()
            val text = when (position) {
                artistHeaderPosition -> "Artists"
                youtubeHeaderPosition -> "YouTube Channels"
                else -> {
                    var offset = 0
                    if (artistHeaderPosition > -1) offset++
                    if (youtubeHeaderPosition > -1 && (position + offset) > youtubeHeaderPosition) offset++

                    val source = sources[position - offset]
                    source.displayName
                }
            }

            holder.itemView.textItem.text = text
        }

        override fun getItemViewType(position: Int): Int {
            val (artistHeaderPosition, youtubeHeaderPosition) = getSectionHeaderPositions()

            return when (position) {
                artistHeaderPosition, youtubeHeaderPosition -> TYPE_HEADER
                else -> TYPE_ITEM
            }
        }

        inner class ReviewQueueItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        inner class ReviewQueueHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
