package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.repository.LAST_POSTED_VERSION_KEY
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.reviewqueue.AddSourceMode
import com.gorilla.gorillagroove.util.Constants
import com.gorilla.gorillagroove.util.sharedPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_more_menu.*

@AndroidEntryPoint
class MoreMenuFragment : Fragment(R.layout.fragment_more_menu) {
    private val controlsViewModel: PlayerControlsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading More Menu view")

        requireActivity().title_tv.text = "More"

        addText.setOnClickListener {
            showAddSongActionSheet()
        }

        usersText.setOnClickListener {
            findNavController().navigate(R.id.usersFragment)
        }

        settingsText.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        logoutButton.setOnClickListener {
            logInfo("User tapped log out")

            logout()

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()

            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }

        menuText.setOnClickListener {
            findNavController().navigate(R.id.problemReportFragment)
        }
    }

    private fun showAddSongActionSheet() {
        ActionSheet(
            requireActivity(),
            listOf(
                ActionSheetItem("Download from YouTube") {
                    findNavController().navigate(
                        R.id.addReviewSourceFragment,
                        bundleOf("MODE" to AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO),
                    )
                },
                ActionSheetItem("Search Spotify") {
                    findNavController().navigate(
                        R.id.addReviewSourceFragment,
                        bundleOf("MODE" to AddSourceMode.SEARCH_SPOTIFY_ARTIST),
                    )
                }
            )
        )
    }

    private fun logout() {
        // There are a lot of prefs that aren't actually cleared out, here. Like all our settings.
        sharedPreferences.edit()
            .remove(Constants.KEY_USER_TOKEN)
            .remove(Constants.KEY_USER_ID)
            .remove(LAST_POSTED_VERSION_KEY)
            .apply()

        controlsViewModel.logout()

        GorillaDatabase.close()
    }
}
