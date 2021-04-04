package com.gorilla.gorillagroove.ui

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbTrack
import kotlinx.android.synthetic.main.track_expandable_item.view.*
import java.util.*
import kotlin.collections.LinkedHashMap

class TrackCellAdapter(
    private val listener: OnTrackListener
) : RecyclerView.Adapter<TrackCellAdapter.PlaylistViewHolder>(), Filterable {

    var trackList = listOf<DbTrack>()
    val filteredList: MutableList<DbTrack> = trackList.toMutableList()
    var playingTrackId: String? = null
    var isPlaying = false

    val checkedTracks = LinkedHashMap<Long, Boolean>()

    var showingCheckBox = false

    fun submitList(tracks: List<DbTrack>) {
        trackList = tracks
        filteredList.clear()
        filteredList.addAll(trackList)
        notifyDataSetChanged()
    }

    fun getSelectedTracks(): List<DbTrack> {
        val checkedTrackIds = checkedTracks.keys

        return trackList.filter { checkedTrackIds.contains(it.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.track_expandable_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = filteredList.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentTrack = filteredList[position]
        holder.tvArtist.text = currentTrack.artist
        holder.tvName.text = currentTrack.name
        holder.tvAlbum.text = currentTrack.album
        holder.tvLength.text = currentTrack.length.getSongTimeFromSeconds()

        val context = holder.itemView.context

        if (currentTrack.id.toString() == playingTrackId) {
            holder.tvName.setTextColor(context.getColor(R.color.ggPrimaryLight))
            holder.tvArtist.setTextColor(context.getColor(R.color.ggPrimary))
            holder.tvAlbum.setTextColor(context.getColor(R.color.ggPrimary))
            holder.tvLength.visibility = View.GONE
            holder.imageButton.visibility = View.VISIBLE

//            listOf(holder.tvName, holder.tvArtist, holder.tvAlbum).forEach { it.setTypeface(it.typeface, Typeface.BOLD) }
        } else {
            holder.tvName.setTextColor(context.getColor(R.color.foreground))
            holder.tvArtist.setTextColor(context.getColor(R.color.grey6))
            holder.tvAlbum.setTextColor(context.getColor(R.color.grey6))
            holder.tvLength.visibility = View.VISIBLE
            holder.imageButton.visibility = View.GONE

            // For whatever dumb reason, it takes Android a while to update the typefaces visually, even though the colors update instantly.
            // This makes it look far more jank than just not having it at all, even though we no longer match the mockup.
//            listOf(holder.tvName, holder.tvArtist, holder.tvAlbum).forEach { it.setTypeface(it.typeface, Typeface.NORMAL) }
        }

        if (isPlaying) {
            holder.imageButton.setImageResource(R.drawable.ic_pause_24)

        } else {
            holder.imageButton.setImageResource(R.drawable.ic_play_arrow_24)
        }

        holder.checkbox.isVisible = showingCheckBox
        holder.options.isVisible = !showingCheckBox
        holder.checkbox.isChecked = checkedTracks[filteredList[position].id] ?: false
        holder.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isShown) {
                buttonView.isChecked = isChecked
                checkedTracks[filteredList[position].id] = isChecked
            }
        }
    }

    private fun Int.getSongTimeFromSeconds(): String {
        val minutes = this / 60
        val seconds = this % 60
        return "$minutes:${String.format("%02d", seconds)}"
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val tvArtist: TextView = itemView.tv_artist
        val tvName: TextView = itemView.tv_title
        val tvAlbum: TextView = itemView.tv_album
        val tvLength: TextView = itemView.tv_length
        val imageButton: ImageButton = itemView.playStatusButton
        val checkbox: CheckBox = itemView.checkbox
        val options: TextView = itemView.tv_options
        val menu_button_parent: ConstraintLayout = itemView.menu_button_layout

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            imageButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onPlayPauseClick(position)
                }
            }
            options.setOnClickListener {
                val position = adapterPosition
                val popup = PopupMenu(itemView.context, it)
                popup.inflate(R.menu.track_floating_menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_play_next -> {
                            listener.onPlayNextSelection(position)
                            true
                        }
                        R.id.action_play_last -> {
                            listener.onPlayLastSelection(position)
                            true
                        }
                        R.id.action_get_link -> {
                            listener.onGetLinkSelection(position)
                            true
                        }
                        R.id.action_download -> {
                            listener.onDownloadSelection(position)
                            true
                        }
                        R.id.action_recommend -> {
                            listener.onRecommendSelection(position)
                            true
                        }
                        R.id.action_add_to_playlist -> {
                            listener.onAddToPlaylistSelection(position)
                            true
                        }
                        R.id.action_properties -> {
                            listener.onPropertiesSelection(position)
                            true
                        }
                        else -> false
                    }
                }

                popup.show()
            }

            expandViewHitArea(menu_button_parent, options)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            //in event of animation
            if (showingCheckBox) {
                checkbox.isChecked = !checkbox.isChecked
            } else {
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTrackClick(position)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onTrackLongClick(position)
            }
            return true
        }

        private fun expandViewHitArea(parent: View, child: View) {
            parent.post {

                val parentRect = Rect()
                val childRect = Rect()
                parent.getHitRect(parentRect)
                child.getHitRect(childRect)

                childRect.left = 0
                childRect.top = 0
                childRect.right = parentRect.width()
                childRect.bottom = parentRect.height()

                parent.touchDelegate = TouchDelegate(childRect, child)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList: List<DbTrack> =
                    if (constraint.isNullOrEmpty()) {
                        trackList
                    } else {
                        val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                        trackList.filter {
                            it.name.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    it.artist.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    it.album.toLowerCase(Locale.ROOT).contains(filterPattern)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList.clear()
                filteredList.addAll(results?.values as List<DbTrack>)
                notifyDataSetChanged()
            }
        }
    }

    interface OnTrackListener {
        fun onTrackClick(position: Int)
        fun onTrackLongClick(position: Int): Boolean
        fun onPlayPauseClick(position: Int)
        fun onOptionsClick(position: Int)

        fun onPlayNextSelection(position: Int)
        fun onPlayLastSelection(position: Int)
        fun onGetLinkSelection(position: Int)
        fun onDownloadSelection(position: Int)
        fun onRecommendSelection(position: Int)
        fun onAddToPlaylistSelection(position: Int)
        fun onPropertiesSelection(position: Int)
    }

//    interface OnOptionsMenuListener {
//        fun onPlayNextSelection(position: Int)
//        fun onPlayLastSelection(position: Int)
//        fun onGetLinkSelection(position: Int)
//        fun onDownloadSelection(position: Int)
//        fun onRecommendSelection(position: Int)
//        fun onAddToPlaylistSelection(position: Int)
//    }


}
