package com.example.gorillagroove.utils

import android.util.Log

@Suppress("unused") // This is very much used. Idk what Kotlin is doing
inline fun<reified T> T.logger() = Logger(T::class.simpleName)

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Logger(private val logTag: String?) {
    fun verbose(message: String) = verbose(logTag, message)
    fun debug(message: String) = debug(logTag, message)
    fun info(message: String) = info(logTag, message)
    fun warn(message: String) = warn(logTag, message)
    fun error(message: String) = error(logTag, message)

    fun verbose(tag: String?, message: String) {
        Log.v(tag ?: logTag, message)
    }
    fun debug(tag: String?, message: String) {
        Log.d(tag ?: logTag, message)
    }
    fun info(tag: String?, message: String) {
        Log.i(tag ?: logTag, message)
    }
    fun warn(tag: String?, message: String) {
        Log.w(tag ?: logTag, message)
    }
    fun error(tag: String?, message: String) {
        Log.e(tag ?: logTag, message)
    }
}
