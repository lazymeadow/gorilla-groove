package com.gorilla.gorillagroove.ui.reviewqueue

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbReviewSource
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.ReviewSourceType
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.CacheType
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.TrackCacheService
import com.gorilla.gorillagroove.ui.GGFragment
import com.gorilla.gorillagroove.ui.PlayerControlsViewModel
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.ui.isPlaying
import com.gorilla.gorillagroove.util.GGToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_review_queue.*
import kotlinx.android.synthetic.main.fragment_track_list.*
import kotlinx.android.synthetic.main.review_queue_carousel_item.view.*
import kotlinx.android.synthetic.main.review_queue_source_select_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import kotlin.math.min


@AndroidEntryPoint
class ReviewQueueFragment : GGFragment(R.layout.fragment_review_queue) {

    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()
    private val reviewSourceDao get() = GorillaDatabase.getDatabase().reviewSourceDao()

    @Inject
    lateinit var mainRepository: MainRepository

    private val playerControlsViewModel: PlayerControlsViewModel by viewModels()

    private lateinit var trackAdapter: ReviewQueueTrackListAdapter
    private lateinit var sourceSelectAdapter: ReviewQueueSourceSelectionAdapter

    private var allReviewSources = mapOf<Long, DbReviewSource>()
    private var sourcesNeedingReview = mutableListOf<DbReviewSource>()
    private var reviewSourceToTrackCount = mutableMapOf<Long, Int>()

    private var activeSource: DbReviewSource? = null

    private var visibleTracks = mutableListOf<DbTrack>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Review Queue view")

        setupTrackRecyclerView()
        setupSourceSelectRecyclerView()

        // This might seem pointless, but apparently you can't initialize a ViewModel on a background thread, or there is something about this particular ViewModel that breaks
        // when you try to do so. Idk why. But in the accept / reject handlers we check the playback state on an IO thread, and if the ViewModel hasn't been started by something
        // else prior, we get a "java.lang.IllegalStateException: Method addObserver must be called on the main thread", even though addObserver is not anywhere in this file,
        // or in the PlaybackControlsViewModel file. Idk I don't get it at all. And I don't want to change the IO thread to be a Main thread because that's even stupider.
        playerControlsViewModel

        setHasOptionsMenu(true)

        reviewQueueSourceSelect.setOnClickListener {
            sourceSelectAdapter.setSources(
                sourcesNeedingReview.map { source -> allReviewSources.getValue(source.id) to (reviewSourceToTrackCount[source.id] ?: 0) },
                activeSource?.id ?: -1
            )

            reviewQueueSelectionList.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()

        // activeSource doesn't seem to get cleared out when navigating BACK from the edit review sources fragment. But some of the UI doesn't remember its state (other parts do?)
        // So just re-init the UI with this same source if we still have it. Why not, right.
        activeSource?.let { source ->
            lifecycleScope.launch {
                setActiveSource(source.id)
            }
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            allReviewSources = reviewSourceDao.findAll().map { it.id to it }.toMap()

            reviewSourceToTrackCount = reviewSourceDao.getNeedingReviewTrackCountByQueue().map { it.reviewSourceId to it.count }.toMap().toMutableMap()
            sourcesNeedingReview = allReviewSources.values.filter { (reviewSourceToTrackCount[it.id] ?: 0) > 0 }.toMutableList()

            setNextActiveSource()
        }
    }

    override fun onBackPressed(): Boolean {
        if (reviewQueueSelectionList.visibility == View.VISIBLE) {
            reviewQueueSelectionList.visibility = View.GONE
            return true
        }
        return super.onBackPressed()
    }

    private fun setupTrackRecyclerView() = reviewQueueTrackList.apply {
        trackAdapter = ReviewQueueTrackListAdapter()
        adapter = trackAdapter
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        PagerSnapHelper().attachToRecyclerView(this)
    }

    private fun setupSourceSelectRecyclerView() = reviewQueueSelectionList.apply {
        sourceSelectAdapter = ReviewQueueSourceSelectionAdapter { newSourceId ->
            lifecycleScope.launch(Dispatchers.Main) {
                setActiveSource(newSourceId)

                reviewQueueSelectionList.visibility = View.GONE
            }
        }

        adapter = sourceSelectAdapter
        addItemDecoration(createDivider(context))
        layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_bar_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_edit_menu -> {
                findNavController().navigate(R.id.editReviewSourcesFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun setNextActiveSource() {
        mainRepository.currentTrack?.takeIf { it.inReview }?.let { currentlyPlayingReviewTrack ->
            val reviewSourceId = currentlyPlayingReviewTrack.reviewSourceId!!
            if (reviewSourceToTrackCount[reviewSourceId] ?: 0 > 0) {
                setActiveSource(reviewSourceId)
                return
            }
        }

        val sourcesByType = sourcesNeedingReview.groupBy { it.sourceType }
        // This is the priority in which we automatically show queues: user -> artist -> youtube
        val sourceToUse = listOf(ReviewSourceType.USER_RECOMMEND, ReviewSourceType.ARTIST, ReviewSourceType.YOUTUBE_CHANNEL).map { sourceType ->
            return@map sourcesByType[sourceType]?.firstOrNull()
        }.filterNotNull().firstOrNull()

        if (sourceToUse == null) {
            // TODO show empty state
        } else {
            setActiveSource(sourceToUse.id)
        }
    }

    private suspend fun setActiveSource(newSourceId: Long) = withContext(Dispatchers.IO) {
        val newActiveSource = allReviewSources.getValue(newSourceId)
        activeSource = newActiveSource

        visibleTracks = trackDao.getTracksNeedingReviewOnSource(newActiveSource.id).toMutableList()
        reviewSourceToTrackCount[newSourceId] = visibleTracks.size // Just make sure it's up to date since we know the real size anyway

        // Though it shouldn't really happen, it's possible that our local state is out of sync because of background things changing tracks.
        // If there ended up not being tracks to review for this source, try the next one
        if (visibleTracks.isEmpty()) {
            setNextActiveSource()
            return@withContext
        }

        // If the queue we're going to already has a track playing, then we should scroll it into view
        val currentlyPlayingTrackIndex = visibleTracks.indexOfFirst { it.id == mainRepository.currentTrack?.id }.takeIf { it > -1 }

        withContext(Dispatchers.Main) {
            reviewQueueSourceSelect.text = newActiveSource.displayName
            trackAdapter.notifyDataSetChanged()
            reviewQueueTrackList.post {
                // Fall back to 0, so that when changing the active source, we start at the far left. Otherwise, your scroll position carries over between changing sources
                val indexToStartAt = currentlyPlayingTrackIndex ?: 0
                reviewQueueTrackList.scrollToPosition(indexToStartAt)
            }
        }
    }

    private suspend fun approveTrack(track: DbTrack) = withContext(Dispatchers.IO) {
        logInfo("User is approving track: ${track.id}")

        val wasPlaying = playerControlsViewModel.playbackState.value.isPlaying

        val reviewSourceId = track.reviewSourceId!!

        try {
            Network.api.approveReviewTrack(track.id)
        } catch (e: Throwable) {
            logError("Failed to approve review track ${track.id}!", e)

            GGToast.show("Failed to approve track")
            return@withContext
        }

        val refreshedTrack = trackDao.findById(track.id) ?: run {
            logError("Failed to find refreshed track with ID ${track.id}!")
            return@withContext
        }

        // This stuff will get synced later with API values. But may as well update it ahead of time since we know roughly what it'll be
        refreshedTrack.inReview = false
        refreshedTrack.addedToLibrary = Instant.now()
        trackDao.save(refreshedTrack)

        playNextAfterReview(reviewSourceId, refreshedTrack, wasPlaying)
    }

    private suspend fun playNextAfterReview(reviewSourceId: Long, track: DbTrack, wasPlaying: Boolean) = withContext(Dispatchers.Main) {
        val newTrackCount = reviewSourceToTrackCount.getValue(reviewSourceId) - 1
        reviewSourceToTrackCount[reviewSourceId] = newTrackCount

        if (newTrackCount == 0) {
            sourcesNeedingReview.removeIf { it.id == reviewSourceId }
        }

        if (activeSource!!.id == reviewSourceId) {
            visibleTracks.removeIf { it.id == track.id }

            if (visibleTracks.isEmpty()) {
                setNextActiveSource()
            } else {
                trackAdapter.notifyDataSetChanged()
            }
        }

        if (wasPlaying && mainRepository.currentIndex < visibleTracks.size) {
            playerControlsViewModel.playMedia(mainRepository.currentIndex, visibleTracks)
        }
    }

    private suspend fun rejectTrack(track: DbTrack) = withContext(Dispatchers.IO) {
        logInfo("User is rejecting track: ${track.id}")

        val wasPlaying = playerControlsViewModel.playbackState.value.isPlaying

        // If someone rejects the song it probably means they really don't want it to keep playing
        if (mainRepository.currentTrack?.id == track.id) {
            playerControlsViewModel.pause()
        }

        val reviewSourceId = track.reviewSourceId!!

        try {
            Network.api.rejectReviewTrack(track.id)
        } catch (e: Throwable) {
            logError("Failed to reject review track ${track.id}!", e)

            GGToast.show("Failed to reject track")
            return@withContext
        }

        trackDao.delete(track.id)

        playNextAfterReview(reviewSourceId, track, wasPlaying)
    }

    inner class ReviewQueueTrackListAdapter : RecyclerView.Adapter<ReviewQueueTrackListAdapter.ReviewQueueItemViewHolder>() {

        private val albumArtCache = mutableMapOf<Long, Bitmap>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewQueueItemViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.review_queue_carousel_item, parent, false
            )
            return ReviewQueueItemViewHolder(itemView)
        }

        override fun getItemCount() = visibleTracks.size

        override fun onBindViewHolder(holder: ReviewQueueItemViewHolder, position: Int) {
            val track = visibleTracks[position]

            holder.setTrack(track, position)
        }

        override fun onViewAttachedToWindow(holder: ReviewQueueItemViewHolder) {
            holder.itemView.playOverlay.visibility = if (holder.currentTrack.id == mainRepository.currentTrack?.id) View.GONE else View.VISIBLE

            super.onViewAttachedToWindow(holder)
        }

        inner class ReviewQueueItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            lateinit var currentTrack: DbTrack

            fun setTrack(track: DbTrack, index: Int) {
                currentTrack = track

                val trackText = listOf(track.name, track.artistString)
                    .filter { it.isNotBlank() }
                    .joinToString(" - ")

                itemView.trackText.text = trackText

                itemView.playOverlay.setOnClickListener {
                    playerControlsViewModel.playMedia(index, visibleTracks)
                    itemView.playOverlay.visibility = View.GONE
                }

                itemView.thumbsDown.setOnClickListener {
                    lifecycleScope.launch {
                        rejectTrack(track)
                    }
                }
                itemView.thumbsUp.setOnClickListener {
                    lifecycleScope.launch {
                        approveTrack(track)
                    }
                }

                setAlbumArt(track)
            }

            @Suppress("BlockingMethodInNonBlockingContext")
            private fun setAlbumArt(track: DbTrack) {
                // Short-term in-memory cache. Just for users swiping around the list left and right
                albumArtCache[track.id]?.let { bitmap ->
                    itemView.albumArt.setImageBitmap(bitmap)
                    itemView.albumArt.visibility = View.VISIBLE
                    return
                }

                // This track has no album art
                if (track.filesizeArt == 0) {
                    return
                }

                itemView.albumArt.visibility = View.GONE

                CoroutineScope(Dispatchers.IO).launch {
                    // Check disk cache. As things are now, I don't think this will ever be populated as review queue isn't available offline in this current iteration. But that could change.
                    TrackCacheService.getCacheItemIfAvailable(track.id, CacheType.ART)?.let { cachedArtFile ->
                        val bitmap = BitmapFactory.decodeFile(cachedArtFile.absolutePath)

                        albumArtCache[track.id] = bitmap

                        withContext(Dispatchers.Main) {
                            itemView.albumArt.setImageBitmap(bitmap)
                            itemView.albumArt.visibility = View.VISIBLE
                        }

                        return@launch
                    }

                    // We're DOING IT LIVE
                    Network.api.getTrackLink(track.id, "LARGE").albumArtLink?.let { artLink ->
                        val artBitmap = Glide.with(GGApplication.application)
                            .asBitmap()
                            .load(artLink)
                            .submit()
                            .get()

                        // Double check the album ID hasn't changed vs the member variable one, because this is a recyclerview and the track we requested art for could have scrolled out of view by now.
                        if (artBitmap != null && track.id == currentTrack.id) {
                            withContext(Dispatchers.Main) {
                                albumArtCache[track.id] = artBitmap
                                itemView.albumArt.setImageBitmap(artBitmap)
                                itemView.albumArt.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }
}

class ReviewQueueSourceSelectionAdapter(
    private val onSourceSelected: (Long) -> Unit
) : RecyclerView.Adapter<ReviewQueueSourceSelectionAdapter.ReviewQueueSourceSelectViewHolder>() {

    private var sourcesWithCount = listOf<Pair<DbReviewSource, Int>>()
    private var selectedSourceId = -1L

    fun setSources(sourcesWithCount: List<Pair<DbReviewSource, Int>>, selectedSourceId: Long) {
        this.sourcesWithCount = sourcesWithCount
        this.selectedSourceId = selectedSourceId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewQueueSourceSelectViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.review_queue_source_select_item, parent, false
        )
        return ReviewQueueSourceSelectViewHolder(itemView)
    }

    override fun getItemCount() = sourcesWithCount.size

    override fun onBindViewHolder(holder: ReviewQueueSourceSelectViewHolder, position: Int) {
        val source = sourcesWithCount[position]

        holder.itemView.setOnClickListener { onSourceSelected(source.first.id) }
        holder.setData(source.first, source.second)
    }

    inner class ReviewQueueSourceSelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun setData(source: DbReviewSource, count: Int) {
            itemView.sourceSelectName.text = source.displayName
            itemView.itemsInQueueText.text = min(count, 99).toString()

            itemView.sourceSelectCheck.visibility = if (source.id == selectedSourceId) View.VISIBLE else View.INVISIBLE
        }
    }
}
