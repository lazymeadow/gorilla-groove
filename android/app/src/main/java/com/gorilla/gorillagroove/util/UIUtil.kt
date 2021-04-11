package com.gorilla.gorillagroove.util

import com.gorilla.gorillagroove.GGApplication

fun getDpFromPixel(pixels: Float): Int {
    val scale: Float = GGApplication.application.resources.displayMetrics.density
    return (pixels * scale + 0.5f).toInt()
}
