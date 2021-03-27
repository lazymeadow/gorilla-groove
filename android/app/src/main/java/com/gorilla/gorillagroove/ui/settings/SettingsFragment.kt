package com.gorilla.gorillagroove.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.LibraryEvent
import com.gorilla.gorillagroove.ui.MainViewModel
import com.gorilla.gorillagroove.ui.PlayerControlsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val viewModel: MainViewModel by viewModels()
    private val controlsViewModel: PlayerControlsViewModel by viewModels()

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Settings view")

        logout_button.setOnClickListener {
            controlsViewModel.logout()

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()

            findNavController().navigate(
                R.id.action_settingsFragment_to_loginFragment,
                savedInstanceState,
                navOptions
            )
        }

        update_tracks_button.setOnClickListener {
            viewModel.setLibraryEvent(LibraryEvent.UpdateAllTracks)
        }

        openProblemReportButton.setOnClickListener {
            findNavController().navigate(R.id.problemReportFragment)
        }
    }
}
