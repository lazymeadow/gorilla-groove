package com.gorilla.gorillagroove.util

import android.content.SharedPreferences
import android.os.Bundle
import androidx.work.Data
import androidx.work.hasKeyWithValueOfType

fun SharedPreferences.getNullableLong(key: String): Long? {
    return if (this.contains(key)) {
        // This technically could have bad stuff happen if someone inserts Long.MIN_VALUE for this key between the contains check and this.
        // If that 0.0001% event happens then well damn. Google should have made a better API. But they made something that's not 100% there like usual.
        this.getLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
    } else {
        null
    }
}

fun Data.getNullableLong(key: String): Long? {
    return if (this.hasKeyWithValueOfType<Long>(key)) {
        this.getLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
    } else {
        null
    }
}

// Why do Bundles not have a way to get a freakin nullable boolean?? Do you expect me to pass everything in as a string if it can be null? Ugh.
fun Bundle.getNullableBoolean(key: String): Boolean? {
    val keySet = this.keySet()
    return if (keySet.contains(key)) {
        this.getBoolean(key)
    } else {
        null
    }
}
