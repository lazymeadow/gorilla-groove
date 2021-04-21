package com.gorilla.gorillagroove.util

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Rect
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.ui.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_input_dialog.view.*


fun getPixelsFromDp(dp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        GGApplication.application.resources.displayMetrics
    ).toInt()
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

fun showEditTextDialog(
    activity: Activity,
    title: String? = null,
    suffix: String? = null,
    yesText: String = "Update",
    noText: String? = "Cancel",
    yesAction: (String) -> Unit = {},
    noAction: () -> Unit = {},
) {
    val builder = AlertDialog.Builder(activity)
    builder.setTitle(title)

    val view = View.inflate(activity, com.gorilla.gorillagroove.R.layout.fragment_input_dialog, null)
    if (suffix != null) {
        view.suffixText.text = suffix
    }

    builder.setView(view)

    builder.setPositiveButton(yesText) { _, _ ->
        val text = view.editText.text.toString()
        if (text.isNotBlank()) {
            yesAction(text)
        }
        view.hideKeyboard()
    }

    if (noText != null) {
        builder.setNegativeButton(noText) { _, _ ->
            noAction()
            view.hideKeyboard()
        }
    }

    val alert = builder.show()

    view.editText.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            view.hideKeyboard()
            alert.dismiss()
            yesAction(view.editText.text.toString())
            true
        } else {
            false
        }
    }

    view.editText.requestFocus()

    view.editText.focusAndShowKeyboard()
}

fun showListSelectDialog(
    activity: Activity,
    title: String? = null,
    options: LinkedHashMap<String, Boolean>, // Boolean is whether or not it should be checked
    yesAction: (String) -> Unit = {},
) {
    val builder = AlertDialog.Builder(activity)
    builder.setTitle(title)

    val optionsArray = options.keys.toTypedArray()
    var checkedItemIndex = optionsArray.indexOfFirst { options.getValue(it) }

    builder.setSingleChoiceItems(optionsArray, checkedItemIndex) { _, selectedIndex ->
        checkedItemIndex = selectedIndex
    }

    builder.setNegativeButton("Cancel") { _, _ -> }
    builder.setPositiveButton("Update") { _, _ ->
        yesAction(optionsArray[checkedItemIndex])
    }

    builder.show()
}
