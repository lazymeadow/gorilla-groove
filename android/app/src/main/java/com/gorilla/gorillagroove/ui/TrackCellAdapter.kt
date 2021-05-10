package com.gorilla.gorillagroove.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import kotlinx.android.synthetic.main.track_expandable_item.view.*
import java.util.*


class TrackCellAdapter<T: TrackReturnable>(
    private val listener: OnTrackListener
) : RecyclerView.Adapter<TrackCellAdapter<T>.PlaylistViewHolder>(), Filterable {

    var trackList = mutableListOf<T>()
    val filteredList: MutableList<T> = trackList.toMutableList()
    var playingTrackId: String? = null
    var isPlaying = false

    private val checkedTrackIds = mutableSetOf<Long>()

    var showingCheckBox = false

    var reorderEnabled = false
    set(value) {
        field = value
        notifyDataSetChanged()
    }

    val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                val movedItem = trackList.removeAt(from)
                trackList.add(to, movedItem)

                filteredList.removeAt(from)
                filteredList.add(to, movedItem)

                notifyItemMoved(from, to)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }

    fun submitList(tracks: List<T>) {
        trackList = tracks.toMutableList()
        filteredList.clear()
        filteredList.addAll(trackList)
        notifyDataSetChanged()
    }

    fun getSelectedTracks(): List<T> {
        return trackList.filter { checkedTrackIds.contains(it.asTrack().id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.track_expandable_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = filteredList.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentTrack = filteredList[position].asTrack()
        holder.tvArtist.text = currentTrack.artistString
        holder.tvName.text = currentTrack.name
        holder.tvAlbum.text = currentTrack.album
        holder.tvLength.text = currentTrack.length.getSongTimeFromSeconds()

        holder.itemView.dragHandle.isVisible = reorderEnabled

        val context = holder.itemView.context

        if (currentTrack.id.toString() == playingTrackId) {
            holder.tvName.setTextColor(context.getColor(R.color.ggPrimaryLight))
            holder.tvArtist.setTextColor(context.getColor(R.color.ggPrimary))
            holder.tvAlbum.setTextColor(context.getColor(R.color.ggPrimary))
            holder.tvLength.setTextColor(context.getColor(R.color.ggPrimary))
        } else {
            holder.tvName.setTextColor(context.getColor(R.color.foreground))
            holder.tvArtist.setTextColor(context.getColor(R.color.grey6))
            holder.tvAlbum.setTextColor(context.getColor(R.color.grey6))
            holder.tvLength.setTextColor(context.getColor(R.color.grey6))
        }

        holder.checkbox.isVisible = showingCheckBox
        holder.checkbox.isChecked = checkedTrackIds.contains(currentTrack.id)
        holder.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isShown) {
                buttonView.isChecked = isChecked
                if (isChecked) {
                    checkedTrackIds.add(currentTrack.id)
                } else {
                    checkedTrackIds.remove(currentTrack.id)
                }
            }
        }

        holder.itemView.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                itemTouchHelper.startDrag(holder)
                return@setOnTouchListener true
            }
            false
        }
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val tvArtist: TextView = itemView.tv_artist
        val tvName: TextView = itemView.tv_title
        val tvAlbum: TextView = itemView.tv_album
        val tvLength: TextView = itemView.tv_length
        val checkbox: CheckBox = itemView.checkbox

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
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
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList: List<T> =
                    if (constraint.isNullOrEmpty()) {
                        trackList
                    } else {
                        val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                        trackList.filter {
                            val track = it.asTrack()

                            track.name.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    track.artist.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    track.featuring.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    track.album.toLowerCase(Locale.ROOT).contains(filterPattern)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList.clear()
                filteredList.addAll(results?.values as List<T>)
                notifyDataSetChanged()
            }
        }
    }

    interface OnTrackListener {
        fun onTrackClick(position: Int)
        fun onTrackLongClick(position: Int)
    }
}

fun Int.getSongTimeFromSeconds(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${String.format("%02d", seconds)}"
}
