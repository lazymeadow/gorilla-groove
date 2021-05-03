package com.gorilla.gorillagroove.service

import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.sync.ServerSynchronizer
import com.gorilla.gorillagroove.service.sync.SyncType
import com.gorilla.gorillagroove.util.GGToast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

object BackgroundTaskService {
    private var idToTask = mutableMapOf<Long, BackgroundTaskItem>()

    private var polling = false

    val currentDownload = MutableStateFlow(0)
    val totalDownloads = MutableStateFlow(0)

    @Synchronized
    fun addBackgroundTasks(tasks: List<BackgroundTaskItem>) {
        logInfo("Adding new background tasks with IDs ${tasks.map { it.id }}")
        tasks.forEach { task ->
            idToTask[task.id] = task
        }

        totalDownloads.value = totalDownloads.value + tasks.size

        if (polling) {
            logDebug("Already polling for tasks. Not starting a new poll")
            return
        } else {
            logDebug("Not already polling for tasks. Starting a new poll")
            polling = true
            GlobalScope.launch(Dispatchers.IO) {
                pollUntilAllProcessed()
            }
        }
    }

    fun pollIfNeeded() {
        if (idToTask.isNotEmpty() && !polling) {
            GlobalScope.launch(Dispatchers.IO) {
                pollUntilAllProcessed()
            }
        }
    }

    private suspend fun pollUntilAllProcessed() {
        val ids = idToTask.values
            .filter { it.status == BackgroundProcessStatus.PENDING || it.status == BackgroundProcessStatus.RUNNING }
            .map { it.id }
            .joinToString(",")

        logDebug("Polling for tasks with IDs $ids")

        val tasks = try {
            Network.api.getActiveBackgroundTasks(ids)
        } catch (e: Throwable) {
            // This will eventually be restarted by pollIfNeeded() being invoked. Probs not ideal but whatevs.
            logError("Could not get background tasks!", e)
            polling = false

            return
        }

        tasks.items.forEach { task ->
            if (task.status == BackgroundProcessStatus.FAILED) {
                logError("Failed to download '${task.description}'")
                GGToast.show("Failed to download '${task.description}'")

                idToTask.remove(task.id)
            } else {
                idToTask[task.id] = task
            }
        }

        var allDone = false
        var completedTasks: MutableCollection<BackgroundTaskItem> = mutableListOf()

        synchronized(this) {
            allDone = idToTask.values.all { it.status == BackgroundProcessStatus.COMPLETE }

            logDebug("Polling is ${if (allDone) "done" else "not done"}")
            if (allDone) {
                completedTasks = idToTask.values

                idToTask = mutableMapOf()
                polling = false
            }
        }

        currentDownload.value = idToTask.filter { it.value.status == BackgroundProcessStatus.COMPLETE }.size
        totalDownloads.value = idToTask.size

        if (allDone) {
            ServerSynchronizer.syncWithServer(setOf(SyncType.TRACK, SyncType.REVIEW_SOURCE), abortIfRecentlySynced = false)

            if (completedTasks.size == 1) {
                GGToast.show("Finished downloading '${completedTasks.first().description}'")
            } else if (completedTasks.size > 1) {
                GGToast.show("Finished downloading ${completedTasks.size} items")
            }
        } else {
            coroutineScope {
                launch {
                    delay(20_000)
                    pollUntilAllProcessed()
                }
            }
        }
    }
}

data class BackgroundTaskItem(
    val id: Long,
    val status: BackgroundProcessStatus,
    val type: BackgroundProcessType,
    val description: String,
)

enum class BackgroundProcessStatus {
    PENDING,
    RUNNING,
    COMPLETE,
    FAILED,
}

enum class BackgroundProcessType {
    YT_DOWNLOAD,
    NAMED_IMPORT,
}

data class BackgroundTaskResponse(val items: List<BackgroundTaskItem>)
