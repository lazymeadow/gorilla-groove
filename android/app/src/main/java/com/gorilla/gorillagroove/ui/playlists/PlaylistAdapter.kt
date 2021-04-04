package com.gorilla.gorillagroove.ui.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import kotlinx.android.synthetic.main.playlists_info_item.view.*
import java.util.*


class PlaylistAdapter(
    private val playlistListener: OnPlaylistListener
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>(), Filterable {

    var playlists = mutableListOf<DbPlaylist>()

    fun submitPlaylistMap(playlists: List<DbPlaylist>) {
        this.playlists.clear()
        this.playlists.addAll(playlists)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.playlists_info_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = playlists.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.playlistName.text = playlists[position].name
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val playlistName: TextView = itemView.playlist_name

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition

            //in event of animation
            if (position != RecyclerView.NO_POSITION) {
                playlistListener.onPlaylistClick(position)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                playlistListener.onPlaylistLongClick(position)
            }
            return true
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList: List<DbPlaylist> =
                    if (constraint.isNullOrEmpty()) {
                        playlists
                    } else {
                        val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                        playlists.filter {
                            it.name.toLowerCase(Locale.ROOT).contains(filterPattern)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }

    interface OnPlaylistListener {
        fun onPlaylistClick(position: Int)
        fun onPlaylistLongClick(position: Int): Boolean
    }
}
