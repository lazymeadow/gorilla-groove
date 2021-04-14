package com.gorilla.gorillagroove.ui

import android.app.ActionBar
import android.app.Activity
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.getDpFromPixel
import kotlinx.android.synthetic.main.dialog_bottom_notification_action.view.*

class ActionSheet(activity: Activity, items: List<ActionSheetItem>) : BottomSheetDialog(activity) {
    init {
        logInfo("Opening action sheet")
        val sheetView = View.inflate(activity, R.layout.dialog_bottom_notification_action, null)
        setContentView(sheetView)
        show()

        // Remove default white color background. Doesn't seem possible to do this in XML
        findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!.background = null

        sheetView.closeButton.setOnClickListener {
            logInfo("Action sheet canceled with 'Cancel' button")
            this.dismiss()
        }

        val container = sheetView.mainActionsContainer

        items.forEachIndexed { index, item ->
            val textView = TextView(ContextThemeWrapper(activity, R.style.ActionSheetItem))
            textView.text = item.text
            textView.setOnClickListener {
                logInfo("Action sheet item selected: '${item.text}'")
                this.dismiss()
                item.onClick()
            }

            container.addView(textView)

            if (index != items.size - 1) {
                val spacer = View(activity)
                val height = getDpFromPixel(1f)
                spacer.layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, height)

                container.addView(spacer)
            }
        }
    }

    override fun cancel() {
        super.cancel()

        logInfo("Action sheet canceled")
    }
}

data class ActionSheetItem(
    val text: String,
    val type: ActionSheetType = ActionSheetType.NORMAL,
    val onClick: () -> Unit,
)

enum class ActionSheetType {
    NORMAL, DESTRUCTIVE
}

fun Fragment.showLibraryActionSheet(track: DbTrack) = ActionSheet(
    requireActivity(),
    listOf(
        ActionSheetItem("Edit Properties") {
            findNavController().navigate(
                R.id.trackPropertiesFragment,
                bundleOf("KEY_TRACK" to track),
            )
        }
    )
)
