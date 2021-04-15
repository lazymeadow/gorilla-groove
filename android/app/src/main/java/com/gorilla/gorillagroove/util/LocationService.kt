package com.gorilla.gorillagroove.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.gorilla.gorillagroove.BuildConfig
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.GGLog.logWarn
import com.gorilla.gorillagroove.ui.settings.GGSettings
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object LocationService {
    fun requestLocationPermissionIfNeeded(activity: Activity) {
        if (!GGSettings.locationConfigured) {
            logInfo("User has not configured whether or not location should be enabled. Prompting with dialog")
            showAlertDialog(
                activity = activity,
                title = "Enable Location?",
                message = "Your location can be saved as part of your listening history",
                yesText = "Yes",
                noText = "No",
                yesAction = {
                    logInfo("User chose to enabled location")
                    GGSettings.locationEnabled = true
                    requestLocationPermissionsIfNeededInternal(activity)
                },
                noAction = {
                    logInfo("User chose to disable location")
                    GGSettings.locationEnabled = false
                }
            )
        } else if (GGSettings.locationEnabled) {
            requestLocationPermissionsIfNeededInternal(activity)
        }
    }

    private fun requestLocationPermissionsIfNeededInternal(activity: Activity) {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // If permissions are already granted, this just does nothing. It's safe to call frequently.
        Dexter.withContext(GGApplication.application)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(result: MultiplePermissionsReport) {
                    if (result.areAllPermissionsGranted()) {
                        logInfo("User accepted location permissions")
                    } else {
                        logInfo("User denied location permissions: ${result.deniedPermissionResponses}")
                    }
                }

                override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                    logInfo("Showing permission rationale")
                    showAlertDialog(
                        activity = activity,
                        title = "Location Permission Needed",
                        message = "You have enabled location saving, but have not granted Gorilla Groove location access.\nPlease enable this in the Android app settings",
                        yesText = "Take me there",
                        noText = "Later",
                        yesAction = { openAppSettings(activity) }
                    )
                }
            })
            .withErrorListener { error ->
                logError("Could not request location permission. Error: $error")
            }
            .check()
    }

    private fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        intent.data = uri
        startActivity(activity, intent, null)
    }

    suspend fun getCurrentLocation(): Location? {
        if (!GGSettings.locationConfigured || !GGSettings.locationEnabled) {
            return null
        }

        val locationPermission = ActivityCompat.checkSelfPermission(GGApplication.application, Manifest.permission.ACCESS_FINE_LOCATION)
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            logWarn("Location is enabled, but permission was not granted.")
            return null
        }

        return suspendCoroutine { coroutine ->
            logDebug("Requesting user's location")

            LocationServices
                .getFusedLocationProviderClient(GGApplication.application)
                .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logDebug("Location was found")
                        coroutine.resume(task.result)
                    } else {
                        logError("Unable to get location point")
                        coroutine.resume(null)
                    }
                }
        }
    }
}
