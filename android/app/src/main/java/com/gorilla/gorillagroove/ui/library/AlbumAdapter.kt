package com.gorilla.gorillagroove.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import kotlinx.android.synthetic.main.simple_text_info_item.view.*
import java.util.*

class AlbumAdapter(
    private val onAlbumTap: (album: String) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>(), Filterable {

    private var albums = listOf<String>()
    private var filteredList = albums.toMutableList()

    fun submitList(albums: List<String>) {
        this.albums = albums
        this.filteredList = albums.toMutableList()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.simple_text_info_item, parent, false
        )
        return AlbumViewHolder(itemView)
    }

    override fun getItemCount() = filteredList.size

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = filteredList[position]
        holder.albumText.text = if (album.isEmpty()) "(No Album)" else album
    }

    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumText: TextView = itemView.textItem

        init {
            itemView.setOnClickListener {
                onAlbumTap(albumText.text.toString())
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(filterString: CharSequence?): FilterResults {
                val resultsList: List<String> =
                    if (filterString.isNullOrEmpty()) {
                        albums
                    } else {
                        val filterPattern = filterString.toString().toLowerCase(Locale.ROOT).trim()
                        albums.filter { album ->
                            album.contains(filterPattern, ignoreCase = true)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList.clear()
                filteredList.addAll(results?.values as List<String>)
                notifyDataSetChanged()
            }
        }
    }
}
