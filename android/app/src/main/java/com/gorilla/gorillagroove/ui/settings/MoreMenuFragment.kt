package com.gorilla.gorillagroove.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.PlayerControlsViewModel
import com.gorilla.gorillagroove.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_more_menu.*
import javax.inject.Inject

@AndroidEntryPoint
class MoreMenuFragment : Fragment(R.layout.fragment_more_menu) {
    private val controlsViewModel: PlayerControlsViewModel by viewModels()

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading More Menu view")

        logoutButton.setOnClickListener {
            logInfo("User tapped log out")

            sharedPref.edit().remove(Constants.KEY_USER_TOKEN).apply()

            controlsViewModel.logout()

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()

            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }

        openProblemReportButton.setOnClickListener {
            findNavController().navigate(R.id.problemReportFragment)
        }
    }
}
