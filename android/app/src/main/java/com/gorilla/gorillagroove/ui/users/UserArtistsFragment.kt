package com.gorilla.gorillagroove.ui.users

import android.os.Bundle
import android.view.View
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.ui.library.ArtistsFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class UserArtistsFragment : ArtistsFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = requireArguments().getSerializable("USER") as DbUser
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title_tv.text = "${user!!.name}'s Library"
    }

    override suspend fun getArtists(): List<String> {
        val trackResponses = Network.api.getTracks(userId = user!!.id, showHidden = showHidden).content

        val uniqueArtists = trackResponses.map { it.artist }.toSet() + trackResponses.map { it.featuring }.toSet()

        return uniqueArtists.sortedBy { it.toLowerCase(Locale.getDefault()) }
    }
}
