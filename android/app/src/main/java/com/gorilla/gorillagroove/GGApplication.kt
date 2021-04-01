package com.gorilla.gorillagroove

import android.app.Application
import android.util.Log
import com.gorilla.gorillagroove.service.GGLog
import com.gorilla.gorillagroove.service.GGLog.logCrit
import com.gorilla.gorillagroove.service.GGLog.logInfo
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class GGApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        application = this

        // Setup handler for uncaught exceptions.

        // Setup handler for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler { _, e -> handleUncaughtException(e) }

        logInfo("\n\nAPP WAS BOOTED\n")

        logInfo("Device is running version ${BuildConfig.VERSION_NAME}")
    }

    private fun handleUncaughtException(e: Throwable) {
        logCrit("Unhandled fatal exception encountered!", e)

        GGLog.flush()

        // TODO send logs once an actual settings menu has been configured and it can be disabled

        // If this is a debug build we probably want to bring down the app so it's very obvious that bad stuff happened
        if (BuildConfig.DEBUG) {
            Log.e("GGApplication", "[APP] Attempting to crash the app because this is a debug build")

            throw Throwable("Uh oh")
        }
    }

    companion object {
        // The fact that so many things on Android require a "context" even though there is no reason for it drives me insane.
        // Why do I need a "context" in order to get a files directory? It's going to be the same for the entire application.
        // Why do I need a "context" to get a string from a strings resource? It's going to be the same for the entire application.
        // Therefore I don't feel bad at all about having a static reference to a guaranteed context even if it is "bad practice".
        // This "bad practice" wouldn't exist if Android wasn't bad, and it is way less bad than passing contexts around pointlessly.
        lateinit var application: GGApplication
    }
}
