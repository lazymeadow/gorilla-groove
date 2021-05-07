package com.gorilla.gorillagroove.ui

import android.app.ActionBar
import android.app.Activity
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.TrackService
import com.gorilla.gorillagroove.util.GGToast
import com.gorilla.gorillagroove.util.getPixelsFromDp
import com.gorilla.gorillagroove.util.showAlertDialog
import kotlinx.android.synthetic.main.dialog_bottom_notification_action.view.*
import kotlinx.coroutines.launch

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

            if (item.type == ActionSheetType.DESTRUCTIVE) {
                textView.setTextColor(activity.getColor(R.color.dangerRed))
            }

            textView.setOnClickListener {
                logInfo("Action sheet item selected: '${item.text}'")
                this.dismiss()
                item.onClick()
            }

            container.addView(textView)

            if (index != items.size - 1) {
                val spacer = View(activity)
                spacer.setBackgroundColor(ContextCompat.getColor(context, R.color.grey1))
                val height = getPixelsFromDp(1f)
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

fun Fragment.showLibraryActionSheet(track: DbTrack) = showLibraryActionSheet(listOf(track))

fun Fragment.showLibraryActionSheet(tracks: List<DbTrack>) = ActionSheet(
    requireActivity(),
    listOf(
        ActionSheetItem("Edit Properties") {
            findNavController().navigate(
                R.id.trackPropertiesFragment,
                bundleOf("KEY_TRACK" to tracks.first()),
            )
        },
        ActionSheetItem("Delete", ActionSheetType.DESTRUCTIVE) {
            showAlertDialog(
                requireActivity(),
                message = "Delete " + (if (tracks.size == 1) tracks.first().name else "the selected ${tracks.size} tracks") + "?",
                yesText = "Delete",
                noText = "Cancel",
                yesAction = {
                    lifecycleScope.launch {
                        val success = TrackService.deleteTracks(tracks)
                        if (success) {
                            // Deletion events are fired that delete the tracks from active views. No need to handle any of that here
                            GGToast.show("Tracks deleted")
                        } else {
                            GGToast.show("Failed to delete tracks")
                        }
                    }
                }
            )
        },
    )
)
