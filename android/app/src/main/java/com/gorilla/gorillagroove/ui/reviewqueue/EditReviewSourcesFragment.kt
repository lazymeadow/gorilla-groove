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
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.*
import com.gorilla.gorillagroove.util.GGToast
import kotlinx.android.synthetic.main.fragment_edit_review_sources.*
import kotlinx.android.synthetic.main.simple_text_info_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditReviewSourcesFragment : GGFragment(R.layout.fragment_edit_review_sources) {

    private val reviewSourceDao get() = GorillaDatabase.getDatabase().reviewSourceDao()

    private lateinit var sourceListAdapter: SourceListAdapter

    private var sources = mutableMapOf<ReviewSourceType, List<DbReviewSource>>()

    // Order of this changes the order that they are shown in
    private val sourceTypesToShow = listOf(ARTIST, YOUTUBE_CHANNEL)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Edit Review Sources view")

        setupTrackRecyclerView()

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.IO) {
            sources = reviewSourceDao.getEditableSources().groupBy { it.sourceType }.toMutableMap()

            sourceTypesToShow.forEach { type ->
                if (sources[type] == null) {
                    sources[type] = emptyList()
                }
            }

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
                        bundleOf("MODE" to AddSourceMode.ADD_SPOTIFY_ARTIST),
                    )
                },
                ActionSheetItem("YouTube Channel") {
                    findNavController().navigate(
                        R.id.addReviewSourceFragment,
                        bundleOf("MODE" to AddSourceMode.ADD_YOUTUBE_CHANNEL),
                    )
                }
            )
        )
    }

    private fun showEditActionSheet(reviewSource: DbReviewSource) {
        ActionSheet(
            requireActivity(),
            listOf(
                ActionSheetItem("Delete", type = ActionSheetType.DESTRUCTIVE) {
                    logInfo("User is attempting to delete review source ${reviewSource.id}")

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            Network.api.deleteReviewSource(reviewSource.id)

                            GorillaDatabase.reviewSourceDao.delete(reviewSource.id)
                            GorillaDatabase.trackDao.deleteTracksOnReviewSource(reviewSource.id)

                            sources[reviewSource.sourceType] = sources.getValue(reviewSource.sourceType).filterNot { it.id == reviewSource.id }

                            withContext(Dispatchers.Main) {
                                sourceListAdapter.notifyDataSetChanged()
                            }
                        } catch (e: Throwable) {
                            logError("Could not delete review source!", e)

                            GGToast.show("Failed to delete ${reviewSource.displayName}")
                        }
                    }
                },
            )
        )
    }

    inner class SourceListAdapter : HeaderTableAdapter<SourceListAdapter.ReviewQueueItemViewHolder>() {

        inner class ReviewQueueItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateBodyViewHolder(parent: ViewGroup): ReviewQueueItemViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.simple_text_info_item, parent, false)
            return ReviewQueueItemViewHolder(itemView)
        }

        override fun getSectionCount() = sources.keys.size

        override fun getCountForSection(sectionIndex: Int): Int {
            val type = sourceTypesToShow[sectionIndex]
            return sources.getValue(type).size
        }

        override fun onBindBodyViewHolder(holder: ReviewQueueItemViewHolder, sectionIndex: Int, positionInSection: Int) {
            val type = sourceTypesToShow[sectionIndex]
            val source = sources.getValue(type)[positionInSection]

            holder.itemView.setOnLongClickListener {
                showEditActionSheet(source)
                true
            }

            holder.itemView.textItem.text = source.displayName
        }

        override fun getTitleForHeader(sectionIndex: Int): String {
            return when (sourceTypesToShow[sectionIndex]) {
                ARTIST -> "Artists"
                YOUTUBE_CHANNEL -> "YouTube Channels"
                else -> throw IllegalStateException("Unknown source type encountered when getting table header text")
            }
        }
    }
}
