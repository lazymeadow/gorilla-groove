package com.gorilla.gorillagroove.ui.settings

import android.content.Context
import android.content.SharedPreferences
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.OfflineModeService
import com.gorilla.gorillagroove.util.Constants
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object GGSettings {
    private val sharedPreferences: SharedPreferences by lazy {
        GGApplication.application.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    var offlineModeEnabled
        get() = sharedPreferences.getBoolean("OFFLINE_MODE_ENABLED", false)
        set(value) {
            logInfo("'OFFLINE_MODE_ENABLED' was set to $value")
            sharedPreferences.edit().putBoolean("OFFLINE_MODE_ENABLED", value).apply()
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

    var offlineStorageEnabled
        get() = sharedPreferences.getBoolean("STORAGE_ENABLED", true)
        set(value) {
            logInfo("'STORAGE_ENABLED' was set to $value")
            // TODO if this is disabled, ask the user if they want to purge all offline data
            // But let's be real, I'll probably never get around to doing this. People can purge the cache in settings if they really want to turn this off and they probably never will want to disable it.
            sharedPreferences.edit().putBoolean("STORAGE_ENABLED", value).apply()
        }

    var offlineStorageMode: OfflineStorageMode
        get() {
            val code = sharedPreferences.getInt("OFFLINE_STORAGE_MODE", OfflineStorageMode.WIFI.storageCode)
            return OfflineStorageMode.findByCode(code)
        }
        set(value) {
            logInfo("'OFFLINE_STORAGE_MODE' was set to $value")
            sharedPreferences.edit().putInt("OFFLINE_STORAGE_MODE", value.storageCode).apply()
        }

    private var lastStorageBytes = sharedPreferences.getLong("MAX_OFFLINE_STORAGE_BYTES", 5_000_000_000L)
    var maximumOfflineStorageBytes = MutableStateFlow(lastStorageBytes)

    var storageWarningSeen
        get() = sharedPreferences.getBoolean("STORAGE_WARNING_SEEN", false)
        set(value) {
            logInfo("'STORAGE_WARNING_SEEN' was set to $value")
            sharedPreferences.edit().putBoolean("STORAGE_WARNING_SEEN", value).apply()
        }

    init {
        GlobalScope.launch {
            maximumOfflineStorageBytes.collect { newStorageBytes ->
                if (newStorageBytes == lastStorageBytes) { return@collect }

                logInfo("'MAX_OFFLINE_STORAGE_BYTES' was set to $newStorageBytes")
                sharedPreferences.edit().putLong("MAX_OFFLINE_STORAGE_BYTES", newStorageBytes).apply()

                storageWarningSeen = false

                OfflineModeService.downloadAlwaysOfflineTracks()
            }
        }
    }
}

enum class OfflineStorageMode(val displayName: String, val storageCode: Int) {
    ALWAYS("Always", 0),
    WIFI("On Wi-Fi", 1),
    NEVER("Never", 2);

    companion object {
        fun findByCode(code: Int): OfflineStorageMode {
            return values().find { it.storageCode == code }
                ?: throw IllegalArgumentException("No OfflineStorageMode found for code $code!")
        }

        fun findByDisplayName(displayName: String): OfflineStorageMode {
            return values().find { it.displayName == displayName }
                ?: throw IllegalArgumentException("No OfflineStorageMode found for displayName $displayName!")
        }
    }
}
