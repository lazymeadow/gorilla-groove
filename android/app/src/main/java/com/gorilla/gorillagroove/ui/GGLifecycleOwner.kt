package com.gorilla.gorillagroove.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.sync.ServerSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GGLifecycleOwner(
    private val serverSynchronizer: ServerSynchronizer
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        logInfo("App has entered the foreground")

        GlobalScope.launch(Dispatchers.IO) {
            serverSynchronizer.syncWithServer(abortIfRecentlySynced = true)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        logInfo("App has entered the background")
    }
}
