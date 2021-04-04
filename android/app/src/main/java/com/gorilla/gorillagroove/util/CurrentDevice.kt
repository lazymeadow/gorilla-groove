package com.gorilla.gorillagroove.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.*

object CurrentDevice {
    // https://stackoverflow.com/a/12707479
    fun getDeviceName(context: Context?): String {
        // If the device has a bluetooth name, it is probably the name that makes the most sense to the user
        context?.let {
            val btName = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
            if (btName?.isNotBlank() == true) {
                return btName
            }
        }

        // Otherwise, fall back to the make and model and hope that makes some amount of sense
        val manufacturer = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: ""
        return if (model.toLowerCase(Locale.ROOT).startsWith(manufacturer.toLowerCase(Locale.ROOT))) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }

    private fun capitalize(s: String): String {
        if (s.isEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }
}
