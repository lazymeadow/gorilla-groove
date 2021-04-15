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

        view.locationEnabledSwitch.isChecked = GGSettings.locationEnabled
        view.locationMinimumBatteryPercent.text = "${GGSettings.locationMinimumBattery}%"

        view.locationEnabledSwitch.setOnCheckedChangeListener { _, isChecked -> GGSettings.locationEnabled = isChecked }
    }
}

object GGSettings {
    private val sharedPreferences: SharedPreferences by lazy {
        GGApplication.application.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    val locationConfigured get() = sharedPreferences.contains("LOCATION_ENABLED")
    var locationEnabled
        get() = sharedPreferences.getBoolean("LOCATION_ENABLED", true)
        set(value) = sharedPreferences.edit().putBoolean("LOCATION_ENABLED", value).apply()

    var locationMinimumBattery
        get() = sharedPreferences.getInt("LOCATION_MINIMUM_BATTERY", 20)
        set(value) = sharedPreferences.edit().putInt("LOCATION_MINIMUM_BATTERY", value).apply()
}
