package com.gorilla.gorillagroove.util

// Just a slightly modified version of the Kotlin "toMap()" function that can be called on an iterable of pairs. This one returns a mutable map instead.
fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
    if (this is Collection) {
        return when (size) {
            0 -> mutableMapOf()
            1 -> mutableMapOf(if (this is List) this[0] else iterator().next())
            else -> toMap(LinkedHashMap())
        }
    }
    return toMap(LinkedHashMap())
}
