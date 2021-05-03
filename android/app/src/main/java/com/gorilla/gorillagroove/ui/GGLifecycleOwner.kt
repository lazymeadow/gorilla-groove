package com.gorilla.gorillagroove.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.service.BackgroundTaskService
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.sync.ServerSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GGLifecycleOwner : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        logInfo("App has entered the foreground")

        if (GGApplication.isUserSignedIn) {
            GlobalScope.launch(Dispatchers.IO) {
                ServerSynchronizer.syncWithServer(abortIfRecentlySynced = true)

                BackgroundTaskService.pollIfNeeded()
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        logInfo("App has entered the background")
    }
}
