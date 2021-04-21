package com.gorilla.gorillagroove.ui.settings

import android.content.Context
import android.content.SharedPreferences
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.Constants

object GGSettings {
    private val sharedPreferences: SharedPreferences by lazy {
        GGApplication.application.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    var backgroundPermissionWarningShown
        get() = sharedPreferences.getBoolean("BACKGROUND_WARNING_SHOWN", false)
        set(value) {
            logInfo("'BACKGROUND_WARNING_SHOWN' was set to $value")
            sharedPreferences.edit().putBoolean("BACKGROUND_WARNING_SHOWN", value).apply()
        }

    val locationConfigured get() = sharedPreferences.contains("LOCATION_ENABLED")
    var locationEnabled
        get() = sharedPreferences.getBoolean("LOCATION_ENABLED", true)
        set(value) {
            logInfo("'LOCATION_ENABLED' was set to $value")
            backgroundPermissionWarningShown = false
            sharedPreferences.edit().putBoolean("LOCATION_ENABLED", value).apply()
        }


    var locationMinimumBattery
        get() = sharedPreferences.getInt("LOCATION_MINIMUM_BATTERY", 20)
        set(value) {
            logInfo("'LOCATION_MINIMUM_BATTERY' was set to $value")
            sharedPreferences.edit().putInt("LOCATION_MINIMUM_BATTERY", value).apply()
        }
}
