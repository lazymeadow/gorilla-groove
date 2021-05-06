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
import com.gorilla.gorillagroove.service.GGSettings
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout


object LocationService {
    fun requestLocationPermissionIfNeeded(activity: Activity) {
        if (!GGSettings.locationConfigured) {
            logInfo("User has not configured whether or not location should be enabled. Prompting with dialog")
            showAlertDialog(
                activity = activity,
                title = "Enable location saving?",
                message = "Your location can be saved with your listening history",
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
                        if (result.deniedPermissionResponses.size == 1) {
                            val deniedPermission = result.deniedPermissionResponses.first().requestedPermission
                            if (deniedPermission.name == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                                showAlertDialog(
                                    activity = activity,
                                    title = "Background Permission Recommended",
                                    message = "Unless permission is granted as 'Always', your location data cannot be included when listening to music with the app in the background.\nWould you like to change it to 'Always'?",
                                    yesText = "Let's change it",
                                    noText = "Later",
                                    yesAction = { openAppSettings(activity) },
                                    noAction = { GGSettings.backgroundPermissionWarningShown = true }
                                )
                            }
                        }
                        logInfo("User denied location permissions: ${result.deniedPermissionResponses}")
                    }
                }

                override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                    if (GGSettings.backgroundPermissionWarningShown) {
                        return
                    }

                    GGSettings.backgroundPermissionWarningShown = true

                    logInfo("Showing permission rationale")
                    showAlertDialog(
                        activity = activity,
                        title = "Location Permission Needed",
                        message = "You have enabled location saving, but have not granted Gorilla Groove enough location access.\nPlease set this to 'Always' in the Android app settings",
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

        // TODO use minimum battery setting!

        val locationPermission = ActivityCompat.checkSelfPermission(GGApplication.application, Manifest.permission.ACCESS_FINE_LOCATION)
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            logWarn("Location is enabled, but permission was not granted.")
            return null
        }

        val cancellationToken = CancellationTokenSource()

        return try {
            withTimeout(5000) {
                logDebug("Requesting user's location")

                val location = LocationServices
                    .getFusedLocationProviderClient(GGApplication.application)
                    .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
                    .await()

                if (location != null) {
                    logDebug("Location was found")
                }

                location
            }
        } catch (e: TimeoutCancellationException) {
            logError("Could not get location within a reasonable time frame. Giving up")
            cancellationToken.cancel()
            null
        }
    }
}
