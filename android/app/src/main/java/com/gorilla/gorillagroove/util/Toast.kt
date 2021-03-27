package com.gorilla.gorillagroove.util

import android.os.Looper
import android.widget.Toast
import com.gorilla.gorillagroove.GGApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GGToast {
    fun show(text: String, duration: Int = Toast.LENGTH_SHORT) {
        // It is inconvenient to invoke the main thread every time we want to log a toast on a background thread.
        // So automatically jump onto the main thread if we do this.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(GGApplication.application, text, duration).show()
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(GGApplication.application, text, duration).show()
            }
        }
    }
}