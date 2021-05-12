package com.gorilla.gorillagroove.util

import android.widget.Toast
import com.gorilla.gorillagroove.GGApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GGToast {
    suspend fun show(text: String, duration: Int = Toast.LENGTH_SHORT): Toast = withContext(Dispatchers.Main) {
        return@withContext Toast.makeText(GGApplication.application, text, duration).also { it.show() }
    }
}