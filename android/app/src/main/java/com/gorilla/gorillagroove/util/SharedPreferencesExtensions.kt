package com.gorilla.gorillagroove.util

import android.content.SharedPreferences

fun SharedPreferences.getNullableLong(key: String): Long? {
    return if (this.contains(key)) {
        // This technically could have bad stuff happen if someone inserts Long.MIN_VALUE for this key between the contains check and this.
        // If that 0.0001% event happens then well damn. Google should have made a better API. But they made something that's not 100% there like usual.
        this.getLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
    } else {
        null
    }
}
