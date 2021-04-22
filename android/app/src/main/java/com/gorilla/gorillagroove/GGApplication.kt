package com.gorilla.gorillagroove

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.gorilla.gorillagroove.service.GGLog
import com.gorilla.gorillagroove.service.GGLog.logCrit
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.sync.ServerSynchronizer
import com.gorilla.gorillagroove.ui.GGLifecycleOwner
import com.gorilla.gorillagroove.util.Constants
import com.gorilla.gorillagroove.util.Constants.KEY_USER_TOKEN
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GGApplication : Application() {

    @Inject
    lateinit var serverSynchronizer: ServerSynchronizer

    override fun onCreate() {
        application = this

        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, e -> handleUncaughtException(e) }

        logInfo("\n\nAPP WAS BOOTED\n")

        logInfo("Device is running version ${BuildConfig.VERSION_NAME}")

        ProcessLifecycleOwner.get().lifecycle.addObserver(GGLifecycleOwner(serverSynchronizer))
    }

    private fun handleUncaughtException(e: Throwable) {
        logCrit("Unhandled fatal exception encountered!", e)

        GGLog.flush()

        // TODO send logs once an actual settings menu has been configured and it can be disabled
    }

    companion object {
        // The fact that so many things on Android require a "context" even though there is no reason for it drives me insane.
        // Why do I need a "context" in order to get a files directory? It's going to be the same for the entire application.
        // Why do I need a "context" to get a string from a strings resource? It's going to be the same for the entire application.
        // Therefore I don't feel bad at all about having a static reference to a guaranteed context even if it is "bad practice".
        // This "bad practice" wouldn't exist if Android wasn't bad, and it is way less bad than passing contexts around pointlessly.
        lateinit var application: GGApplication

        val isUserSignedIn get() = application
            .getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_TOKEN, null)
            .takeIf { !it.isNullOrBlank() } != null
    }
}
