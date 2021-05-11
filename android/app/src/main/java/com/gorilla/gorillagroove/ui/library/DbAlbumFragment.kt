package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.dao.Album
import com.gorilla.gorillagroove.service.TrackService
import kotlinx.android.synthetic.main.activity_main.*

class DbAlbumFragment : AlbumFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requireActivity().title_tv.text = "My Library"

        super.onCreate(savedInstanceState)
    }

    override suspend fun getAlbums(): List<Album> {
        val includeHidden = if (showHidden) null else false

        return GorillaDatabase.trackDao.getDistinctAlbums(
            artistFilter = TrackService.getArtistSqlFilter(artistFilter),
            isHidden = includeHidden,
            inReview = false
        )
    }
}
