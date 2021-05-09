package com.gorilla.gorillagroove.ui.library

import com.gorilla.gorillagroove.database.GorillaDatabase

class DbArtistsFragment : ArtistsFragment() {

    override suspend fun getArtists(): List<String> {
        val includeHidden = if (showHidden) null else false
        return GorillaDatabase.trackDao.getDistinctArtists(isHidden = includeHidden, inReview = false)
    }
}
