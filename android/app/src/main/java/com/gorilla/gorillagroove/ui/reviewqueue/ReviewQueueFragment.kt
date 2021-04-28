package com.gorilla.gorillagroove.ui.reviewqueue

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbReviewSource
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.CacheType
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.TrackCacheService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_review_queue.*
import kotlinx.android.synthetic.main.review_queue_carousel_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class ReviewQueueFragment : Fragment(R.layout.fragment_review_queue) {

    private val trackDao get() = GorillaDatabase.getDatabase().trackDao()
    private val reviewSourceDao get() = GorillaDatabase.getDatabase().reviewSourceDao()

    private lateinit var trackAdapter: ReviewQueueTrackListAdapter

    private var allReviewSources = listOf<DbReviewSource>()
    private var sourcesNeedingReview = mutableListOf<DbReviewSource>()

    private var activeSource: DbReviewSource? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Review Queue view")

        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.IO) {
            allReviewSources = reviewSourceDao.findAll()

            val reviewCounts = reviewSourceDao.getNeedingReviewTrackCountByQueue().map { it.reviewSourceId to it.count }.toMap()
            sourcesNeedingReview = allReviewSources.filter { (reviewCounts[it.id] ?: 0) > 0 }.toMutableList()

            withContext(Dispatchers.Main) {
                if (sourcesNeedingReview.isNotEmpty()) {
                    activeSource = sourcesNeedingReview.first().also {
                        reviewQueueSourceSelect.text = it.displayName
                    }

                    withContext(Dispatchers.IO) {
                        val tracks = trackDao.getTracksNeedingReviewOnSource(activeSource!!.id)

                        withContext(Dispatchers.Main) {
                            trackAdapter.setTracks(tracks)
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() = reviewQueueTrackList.apply {
        trackAdapter = ReviewQueueTrackListAdapter()
        adapter = trackAdapter
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        PagerSnapHelper().attachToRecyclerView(this)
    }
}

class ReviewQueueTrackListAdapter : RecyclerView.Adapter<ReviewQueueTrackListAdapter.ReviewQueueItemViewHolder>() {

    private var tracks = listOf<DbTrack>()

    private val albumArtCache = mutableMapOf<Long, Bitmap>()

    fun setTracks(tracks: List<DbTrack>) {
        logDebug("Setting tracks")
        this.tracks = tracks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewQueueItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.review_queue_carousel_item, parent, false
        )
        return ReviewQueueItemViewHolder(itemView)
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: ReviewQueueItemViewHolder, position: Int) {
        val track = tracks[position]

        holder.setTrack(track)
    }

    inner class ReviewQueueItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private lateinit var currentTrack: DbTrack

        fun setTrack(track: DbTrack) {
            currentTrack = track

            var artist = track.artist
            if (track.featuring.isNotEmpty()) {
                artist += " ft. ${track.featuring}"
            }

            val trackText = listOf(track.name, artist)
                .filter { it.isNotBlank() }
                .joinToString(" - ")

            itemView.trackText.text = trackText

            setAlbumArt(track)
        }

        private fun setAlbumArt(track: DbTrack) {
            // This track has no album art
            if (track.filesizeArt == 0) {
                return
            }

            // Short-term in-memory cache. Just for users swiping around the list left and right
            albumArtCache[track.id]?.let { bitmap ->
                itemView.albumArt.setImageBitmap(bitmap)
                return
            }

            val musicNoteDrawable = ResourcesCompat.getDrawable(GGApplication.application.resources, R.drawable.ic_music, null)
            itemView.albumArt.setImageDrawable(musicNoteDrawable)

            CoroutineScope(Dispatchers.IO).launch {
                // Check disk cache. As things are now, I don't think this will ever be populated as review queue isn't available offline in this current iteration. But that could change.
                TrackCacheService.getCacheItemIfAvailable(track.id, CacheType.ART)?.let { cachedArtFile ->
                    val bitmap = BitmapFactory.decodeFile(cachedArtFile.absolutePath)

                    albumArtCache[track.id] = bitmap

                    withContext(Dispatchers.Main) {
                        itemView.albumArt.setImageBitmap(bitmap)
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
                        }
                    }
                }
            }
        }
    }
}
