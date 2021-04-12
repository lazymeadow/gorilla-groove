package com.gorilla.gorillagroove.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import kotlinx.android.synthetic.main.simple_text_info_item.view.*
import java.util.*

class ArtistsAdapter(
    private val onArtistTap: (artist: String) -> Unit
) : RecyclerView.Adapter<ArtistsAdapter.ArtistViewHolder>(), Filterable {

    private var artists = listOf<String>()
    private var filteredList = artists.toMutableList()

    fun submitList(artists: List<String>) {
        this.artists = artists
        this.filteredList = artists.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.simple_text_info_item, parent, false
        )
        return ArtistViewHolder(itemView)
    }

    override fun getItemCount() = filteredList.size

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = filteredList[position]
        holder.artistText.text = if (artist.isEmpty()) "(No Artist)" else artist
    }

    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artistText: TextView = itemView.textItem

        init {
            itemView.setOnClickListener {
                onArtistTap(artistText.text.toString())
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(filterString: CharSequence?): FilterResults {
                val resultsList: List<String> =
                    if (filterString.isNullOrEmpty()) {
                        artists
                    } else {
                        val filterPattern = filterString.toString().toLowerCase(Locale.ROOT).trim()
                        artists.filter { artist ->
                            artist.contains(filterPattern, ignoreCase = true)
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
