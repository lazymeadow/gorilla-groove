package com.gorilla.gorillagroove.util

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Rect
import android.text.Editable
import android.text.InputType.*
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.ui.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_input_dialog.view.*
import kotlinx.coroutines.*


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

fun EditText.addDebounceTextListener(
    scope: CoroutineScope,
    onDebounceStart: () -> Unit,
    onDebounceEnd: (String) -> Unit
) {
    var searchJob: Job? = null

    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            searchJob?.cancel()
            onDebounceStart()

            searchJob = scope.launch(Dispatchers.IO) {
                val newValue = s?.toString() ?: ""

                delay(500)
                onDebounceEnd(newValue)
            }
        }
    })
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
    keyboardMode: KeyboardMode = KeyboardMode.TEXT,
    yesAction: (String) -> Unit = {},
    noAction: () -> Unit = {},
) {
    val builder = AlertDialog.Builder(activity)
    builder.setTitle(title)

    val view = View.inflate(activity, com.gorilla.gorillagroove.R.layout.fragment_input_dialog, null)
    if (suffix != null) {
        view.suffixText.text = suffix
    }

    view.editText.inputType = keyboardMode.xmlType
    (view.editText.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMinWidth = getPixelsFromDp(keyboardMode.maxWidth.toFloat())

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

// It's lazy to couple the maxWidth to the type of keyboard. But lazy is what you're working with, codebase.
enum class KeyboardMode(val xmlType: Int, val maxWidth: Int) {
    NUMBER_DECIMAL(TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL, 80),
    TEXT(TYPE_CLASS_TEXT, 200),
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

class ShowAlertDialogRequest(
    val title: String? = null,
    val message: String? = null,
    val yesText: String? = null,
    val noText: String? = null,
    val yesAction: () -> Unit = {},
    val noAction: () -> Unit = {},
)
