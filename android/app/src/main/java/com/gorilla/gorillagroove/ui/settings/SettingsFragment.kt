package com.gorilla.gorillagroove.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.view.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Settings view")

        view.locationEnabledSwitch.isEnabled = locationEnabled
        view.locationMinimumBatteryPercent.text = "$locationMinimumBattery%"
    }

    companion object {
        private val sharedPreferences: SharedPreferences by lazy {
            GGApplication.application.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }

        val locationEnabled = sharedPreferences.getBoolean("LOCATION_ENABLED", true)
        val locationMinimumBattery = sharedPreferences.getInt("LOCATION_MINIMUM_BATTERY", 20)
    }
}
