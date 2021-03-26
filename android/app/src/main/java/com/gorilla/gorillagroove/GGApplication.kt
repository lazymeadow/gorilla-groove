package com.gorilla.gorillagroove

import android.app.Application
import android.util.Log
import com.gorilla.gorillagroove.service.GGLog
import com.gorilla.gorillagroove.service.GGLog.logCrit
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class GGApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        application = this

        // Setup handler for uncaught exceptions.

        // Setup handler for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler { thread, e -> handleUncaughtException(thread, e) }
    }

    private fun handleUncaughtException(thread: Thread, e: Throwable) {
        logCrit("Unhandled fatal exception encountered!", e)

        GGLog.flush()

        // TODO send logs

        // If this is a debug build we probably want to bring down the app so it's very obvious that bad stuff happened
        if (BuildConfig.DEBUG) {
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, e) ?: run {
                // Use the android logger here because we are unlikely to actually have the time to log this to a file with our actual logger before the app crashes anyway.
                Log.wtf("GGApplication", "No default uncaught exception handler was defined. Crashing the app the old-fashioned way")

                val uhOhNull = null
                @Suppress("ALWAYS_NULL")
                uhOhNull!!
            }
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
