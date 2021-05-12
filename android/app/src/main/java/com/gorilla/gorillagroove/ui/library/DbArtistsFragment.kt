package com.gorilla.gorillagroove.ui.library

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.database.GorillaDatabase
import kotlinx.android.synthetic.main.activity_main.*

class DbArtistsFragment : ArtistsFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title_tv.text = "My Library"
    }

    override suspend fun getArtists(): List<String> {
        val includeHidden = if (showHidden) null else false
        return GorillaDatabase.trackDao.getDistinctArtists(isHidden = includeHidden, inReview = false)
    }
}
