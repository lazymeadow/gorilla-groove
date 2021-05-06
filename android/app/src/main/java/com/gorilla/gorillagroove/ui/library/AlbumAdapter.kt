package com.gorilla.gorillagroove.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.dao.Album
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.ui.settings.GGSettings
import kotlinx.android.synthetic.main.album_info_item.view.*
import kotlinx.coroutines.*
import java.util.*

class AlbumAdapter(
    private val onAlbumTap: (album: Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>(), Filterable {

    private var albums = listOf<Album>()
    private var filteredList = albums.toMutableList()

    fun submitList(albums: List<Album>) {
        this.albums = albums
        this.filteredList = albums.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.album_info_item, parent, false
        )
        return AlbumViewHolder(itemView)
    }

    override fun getItemCount() = filteredList.size

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.setAlbum(filteredList[position])
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val albumText: TextView = itemView.textItem

        // Boo not having custom setters on lateinit properties
        lateinit var mAlbum: Album
        fun setAlbum(album: Album) {
            mAlbum = album
            albumText.text = if (album.name.isEmpty()) "(No Album)" else album.name

            mAlbum.imageData?.let { image ->
                itemView.albumArt.setImageBitmap(image)
            } ?: run {
                itemView.albumArt.setImageBitmap(null)
                if (!album.albumArtFetched && !GGSettings.offlineModeEnabled) {
                    CoroutineScope(Dispatchers.IO).launch {
                        Network.api.getTrackLink(album.trackId, "SMALL").albumArtLink?.let { artLink ->
                            val artBitmap = Glide.with(GGApplication.application).applyDefaultRequestOptions(glideOptions)
                                .asBitmap()
                                .load(artLink)
                                .submit()
                                .get()

                            album.imageData = artBitmap
                            // Double check the album ID hasn't changed vs the member variable one, because this is a recyclerview and the track we requested art for could have scrolled out of view by now.
                            if (artBitmap != null && album.trackId == mAlbum.trackId) {
                                withContext(Dispatchers.Main) {
                                    itemView.albumArt.setImageBitmap(artBitmap)
                                }
                            }
                        }
                    }
                }
            }
        }

        init {
            itemView.setOnClickListener {
                onAlbumTap(mAlbum)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(filterString: CharSequence?): FilterResults {
                val resultsList: List<Album> =
                    if (filterString.isNullOrEmpty()) {
                        albums
                    } else {
                        val filterPattern = filterString.toString().toLowerCase(Locale.ROOT).trim()
                        albums.filter { album ->
                            album.name.contains(filterPattern, ignoreCase = true)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList.clear()
                filteredList.addAll(results?.values as List<Album>)
                notifyDataSetChanged()
            }
        }
    }
}

private val glideOptions = RequestOptions()
    .fallback(R.drawable.blue)
    .diskCacheStrategy(DiskCacheStrategy.NONE)
