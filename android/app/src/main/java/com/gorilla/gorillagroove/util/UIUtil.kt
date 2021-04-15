package com.gorilla.gorillagroove.util

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.ui.MainActivity
import kotlinx.android.synthetic.main.activity_main.*

fun getDpFromPixel(pixels: Float): Int {
    val scale: Float = GGApplication.application.resources.displayMetrics.density
    return (pixels * scale + 0.5f).toInt()
}


fun View.containsMotionEvent(parentView: View, activity: MainActivity, event: MotionEvent): Boolean {
    val bounds = Rect().apply { getHitRect(this) }
    val locationOnScreen = IntArray(2)

    activity.activity_main_root.getLocationOnScreen(locationOnScreen)
    val (activityOffsetWidth, activityOffsetHeight) = locationOnScreen

    parentView.getLocationOnScreen(locationOnScreen)
    val (viewOffsetWidth, viewOffsetHeight) = locationOnScreen

    val effectiveWidth = event.x.toInt() + activityOffsetWidth - viewOffsetWidth
    val effectiveHeight = event.y.toInt() + activityOffsetHeight - viewOffsetHeight

    return (bounds.contains(effectiveWidth, effectiveHeight))
}

fun showAlertDialog(
    activity: Activity,
    title: String? = null,
    message: String? = null,
    yesText: String? = null,
    noText: String? = null,
    yesAction: () -> Unit = {},
    noAction: () -> Unit = {},
) {
    val builder = AlertDialog.Builder(activity)
        .setTitle(title)
        .setMessage(message)

    if (yesText != null) {
        builder.setPositiveButton(yesText) { _, _ ->
            yesAction()
        }
    }

    if (noText != null) {
        builder.setNegativeButton(noText) { _, _ ->
            noAction()
        }
    }

    // Create the AlertDialog
    builder.create().show()
}


