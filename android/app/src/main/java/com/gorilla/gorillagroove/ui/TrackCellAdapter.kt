package com.gorilla.gorillagroove.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbTrack
import kotlinx.android.synthetic.main.track_expandable_item.view.*
import java.util.*

class TrackCellAdapter(
    private val listener: OnTrackListener
) : RecyclerView.Adapter<TrackCellAdapter.PlaylistViewHolder>(), Filterable {

    var trackList = listOf<DbTrack>()
    val filteredList: MutableList<DbTrack> = trackList.toMutableList()
    var playingTrackId: String? = null
    var isPlaying = false

    val checkedTrackIds = mutableSetOf<Long>()

    var showingCheckBox = false

    fun submitList(tracks: List<DbTrack>) {
        trackList = tracks
        filteredList.clear()
        filteredList.addAll(trackList)
        notifyDataSetChanged()
    }

    fun getSelectedTracks(): List<DbTrack> {
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
        holder.tvArtist.text = currentTrack.artistString
        holder.tvName.text = currentTrack.name
        holder.tvAlbum.text = currentTrack.album
        holder.tvLength.text = currentTrack.length.getSongTimeFromSeconds()

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
                val resultsList: List<DbTrack> =
                    if (constraint.isNullOrEmpty()) {
                        trackList
                    } else {
                        val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                        trackList.filter {
                            it.name.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    it.artist.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    it.featuring.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                                    it.album.toLowerCase(Locale.ROOT).contains(filterPattern)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList.clear()
                filteredList.addAll(results?.values as List<DbTrack>)
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
