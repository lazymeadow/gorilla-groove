@file:Suppress("unused")

package com.gorilla.gorillagroove.service

import android.util.Log
import com.gorilla.gorillagroove.BuildConfig
import com.gorilla.gorillagroove.GGApplication
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timer

object GGLog {
    private const val MAX_LOG_SIZE = 5_242_880 // 5 MiB

    private var logBuffer = LinkedList<String>()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    private val minimumLogLevel = LogLevel.DEBUG

    private var logsInit = false

    private var logFile1: File? = null
    private var logFile2: File? = null
    private var activeLogFile: File? = null

    private val logsDirPath: String by lazy { "${GGApplication.application.filesDir.absolutePath}/logs" }
    private val logFile1Path: String by lazy { "$logsDirPath/gglog1.txt" }
    private val logFile2Path: String by lazy { "$logsDirPath/gglog2.txt" }

    init {
        // Writing log files out to disk is relatively expensive, and are only used when someone is viewing on-device logs or sending in a problem report.
        // As such, we hold the logs in a buffer and periodically dump them to the file in bulk on a background thread.
        timer(
            daemon = true,
            initialDelay = 250,
            period = 500
        ) {
            flush()
        }
    }

    fun Any.logVerbose(message: String) = logMessage(this.logTag, message, LogLevel.VERBOSE)
    fun Any.logDebug(message: String) = logMessage(this.logTag, message, LogLevel.DEBUG)
    fun Any.logInfo(message: String) = logMessage(this.logTag, message, LogLevel.INFO)
    fun Any.logWarn(message: String) = logMessage(this.logTag, message, LogLevel.WARNING)
    fun Any.logError(message: String) = logMessage(this.logTag, message, LogLevel.ERROR)
    fun Any.logError(message: String, e: Throwable) = logMessage(this.logTag, message + "\n${Log.getStackTraceString(e)}", LogLevel.ERROR)
    fun Any.logCrit(message: String) = logMessage(this.logTag, message, LogLevel.CRITICAL)
    fun Any.logCrit(message: String, e: Throwable) = logMessage(this.logTag, message + "\n${Log.getStackTraceString(e)}", LogLevel.CRITICAL)

    // I ran into situations where some stuff had no simpleName, but their enclosing classes did.
    private val Any.logTag: String get() {
        var currentClass: Class<*> = this.javaClass
        while (currentClass.simpleName.isEmpty()) {
            currentClass = currentClass.enclosingClass ?: return "UNKNOWN"
        }

        return currentClass.simpleName
    }

    private fun logMessage(tag: String, message: String, logLevel: LogLevel) {
        if (logLevel.priority < minimumLogLevel.priority) {
            return
        }

        // No reason to log to logcat in production. Needless CPU cycles
        if (BuildConfig.DEBUG) {
            // Logcat is awful and displays log messages from all libraries we use without letting us easily filter them all out.
            // So this is a dumb hack to allow us to easily configure logcat's regex to only show logs from our own system.
            val logcatMessage = "[APP] $message"
            when (logLevel) {
                LogLevel.VERBOSE -> Log.v(tag, logcatMessage)
                LogLevel.DEBUG -> Log.d(tag, logcatMessage)
                LogLevel.INFO -> Log.i(tag, logcatMessage)
                LogLevel.WARNING -> Log.w(tag, logcatMessage)
                LogLevel.ERROR -> Log.e(tag, logcatMessage)
                LogLevel.CRITICAL -> Log.wtf(tag, logcatMessage)
            }
        }

        val fileMessage = "${LocalDateTime.now().format(formatter)} [$tag] [${logLevel.logName}]: $message"

        synchronized(formatter) {
            logBuffer.add(fileMessage)
        }
    }

    @Synchronized
    fun flush() {
        val statementsToLog = synchronized(formatter) {
            if (logBuffer.isEmpty()) {
                return
            }

            val buffer = logBuffer
            logBuffer = LinkedList()

            buffer
        }

        val activeLogFile = getFileToLog()

        PrintWriter(FileOutputStream(activeLogFile, true)).use { writer ->
            statementsToLog.forEach { writer.println(it) }
        }
    }

    private fun initLogFilesIfNeeded() {
        if (logsInit) {
            return
        }

        val logsDir = File(logsDirPath)
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }

        logFile1 = File(logFile1Path)
        logFile2 = File(logFile2Path)

        // createNewFile() does nothing if the file already exists. So this is fine to do.
        logFile1?.createNewFile()
        logFile2?.createNewFile()

        logsInit = true
    }

    private fun getFileToLog(): File {
        initLogFilesIfNeeded()

        // Checking if the file exists and its modified date are relatively expensive as they hit the disk. So we cache the active file.
        activeLogFile?.let { activeLogFile ->
            if (activeLogFile.length() < MAX_LOG_SIZE) {
                return activeLogFile
            }
        }

        val (fileToLog, backupFile) = getActiveAndBackupLogFile()

        return if (fileToLog.length() > MAX_LOG_SIZE) {
            logInfo("Log file length ${fileToLog.length()} exceeds the log length limit of $MAX_LOG_SIZE")
            // Should always exist. But paranoia
            if (backupFile.exists()) {
                backupFile.delete()
            }
            backupFile.createNewFile()

            activeLogFile = backupFile

            logInfo("Log file was rotated. It is now logging to ${backupFile.name}")

            backupFile
        } else {
            activeLogFile = fileToLog

            fileToLog
        }
    }

    private fun getActiveAndBackupLogFile(): Pair<File, File> {
        val logFile1 = logFile1 ?: throw IllegalStateException("logFile1 does not exist!")
        val logFile2 = logFile2 ?: throw IllegalStateException("logFile2 does not exist!")

        return if (logFile1.lastModified() >= logFile2.lastModified()) {
            logFile1 to logFile2
        } else {
            logFile2 to logFile1
        }
    }

    fun getLogContent(): List<String> {
        val (fileToLog, backupFile) = getActiveAndBackupLogFile()

        flush()

        // Backup file will have older content, so it needs to go first
        return backupFile.readLines() + fileToLog.readLines()
    }
}

enum class LogLevel(val priority: Int, val logName: String) {
    VERBOSE(0, "trace"), // I'm calling it trace because that's what the API and iOS calls it and these logs are sent in to the same place. It doesn't REALLY matter though.
    DEBUG(1, "debug"),
    INFO(2, "info"),
    WARNING(3, "warn"),
    ERROR(5, "error"),
    CRITICAL(6, "crit")
}
