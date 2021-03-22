@file:Suppress("unused")

package com.gorilla.gorillagroove.service

import android.util.Log
import com.gorilla.gorillagroove.BuildConfig

object GGLog {
    fun Any.logVerbose(message: String) = logMessage(this.javaClass.simpleName, message, LogLevel.VERBOSE)
    fun Any.logDebug(message: String) = logMessage(this.javaClass.simpleName, message, LogLevel.DEBUG)
    fun Any.logInfo(message: String) = logMessage(this.javaClass.simpleName, message, LogLevel.INFO)
    fun Any.logWarn(message: String) = logMessage(this.javaClass.simpleName, message, LogLevel.WARNING)
    fun Any.logError(message: String) = logMessage(this.javaClass.simpleName, message, LogLevel.ERROR)
    fun Any.logError(message: String, e: Throwable) = logMessage(this.javaClass.simpleName, message + "\n${Log.getStackTraceString(e)}", LogLevel.ERROR)
    fun Any.logCrit(message: String) = logMessage(this.javaClass.simpleName, message, LogLevel.CRITICAL)
    fun Any.logCrit(message: String, e: Throwable) = logMessage(this.javaClass.simpleName, message + "\n${Log.getStackTraceString(e)}", LogLevel.CRITICAL)

    private fun logMessage(tag: String, message: String, logLevel: LogLevel) {
        // No reason to log to logcat in production. Needless CPU cycles
        if (BuildConfig.DEBUG) {
            when (logLevel) {
                LogLevel.VERBOSE -> Log.v(tag, message)
                LogLevel.DEBUG -> Log.d(tag, message)
                LogLevel.INFO -> Log.i(tag, message)
                LogLevel.WARNING -> Log.w(tag, message)
                LogLevel.ERROR -> Log.e(tag, message)
                LogLevel.CRITICAL -> Log.wtf(tag, message)
            }
        }
    }
}

enum class LogLevel(priority: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(5),
    CRITICAL(6)
}
