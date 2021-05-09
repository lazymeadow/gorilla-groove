package com.gorilla.gorillagroove.ui.library

import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.dao.Album

class DbAlbumFragment : AlbumFragment() {
    override suspend fun getAlbums(): List<Album> {
        val includeHidden = if (showHidden) null else false
        return GorillaDatabase.trackDao.getDistinctAlbums(artistFilter = artistFilter, isHidden = includeHidden, inReview = false)
    }
}
