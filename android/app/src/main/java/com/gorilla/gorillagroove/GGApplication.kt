package com.gorilla.gorillagroove

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import com.gorilla.gorillagroove.service.DynamicTrackAudioCacheSource
import com.gorilla.gorillagroove.service.GGLog
import com.gorilla.gorillagroove.service.GGLog.logCrit
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.GGLifecycleOwner
import com.gorilla.gorillagroove.util.Constants
import com.gorilla.gorillagroove.util.Constants.KEY_USER_TOKEN
import com.gorilla.gorillagroove.util.getNullableLong
import com.gorilla.gorillagroove.util.sharedPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@HiltAndroidApp
class GGApplication : Application() {

    override fun onCreate() {
        application = this

        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, e -> handleUncaughtException(e) }

        initWorkManager()

        logInfo("\n\nAPP WAS BOOTED\n")

        logInfo("Device is running version ${BuildConfig.VERSION_NAME}")

        GlobalScope.launch(Dispatchers.IO) {
            // Give the app some breathing room while it's booting
            delay(3_000)

            DynamicTrackAudioCacheSource.purgeCache()

            withContext(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(GGLifecycleOwner())
            }
        }
    }

    // WorkManager is insane and will try to run a ridiculous amount of things in parallel.
    // Even if all of these things are on the background, what happens is the entire device ends up
    // getting hammered to the point that it can barely do anything. So give it a very limited thread pool.
    private fun initWorkManager() {
        val configuration = Configuration.Builder()
            .setExecutor(Executors.newFixedThreadPool(1))
            .build()

        WorkManager.initialize(this, configuration)
    }

    private fun handleUncaughtException(e: Throwable) {
        logCrit("Unhandled fatal exception encountered!", e)

        GGLog.flush()
    }

    companion object {
        // The fact that so many things on Android require a "context" even though there is no reason for it drives me insane.
        // Why do I need a "context" in order to get a files directory? It's going to be the same for the entire application.
        // Why do I need a "context" to get a string from a strings resource? It's going to be the same for the entire application.
        // Therefore I don't feel bad at all about having a static reference to a guaranteed context even if it is "bad practice".
        // This "bad practice" wouldn't exist if Android wasn't bad, and it is way less bad than passing contexts around pointlessly.
        lateinit var application: GGApplication

        val isUserSignedIn get() = sharedPreferences.getString(KEY_USER_TOKEN, null)
            .takeIf { !it.isNullOrBlank() } != null

        val loggedInUserId get() = sharedPreferences.getNullableLong(Constants.KEY_USER_ID)
    }
}
