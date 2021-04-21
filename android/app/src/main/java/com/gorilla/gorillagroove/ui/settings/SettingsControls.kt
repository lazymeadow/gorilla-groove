// IDK. Android's style stuff seems bad. You can't have the same style name with two different components.
// So I made them use the same component, which then obviously couldn't have the same name for both components.
// I have no idea how you get styled attributes that are top level in the attrs.xml file. Surely this is possible,
// and would probably be a better solution, but nobody on Google has the answer and I'm sick of this.
@file:SuppressLint("CustomViewStyleable")

package com.gorilla.gorillagroove.ui.settings

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.util.getPixelsFromDp
import com.gorilla.gorillagroove.util.showEditTextDialog
import com.gorilla.gorillagroove.util.showListSelectDialog
import dagger.hilt.android.internal.managers.FragmentComponentManager.findActivity
import kotlinx.android.synthetic.main.setting_control_group.view.*
import kotlinx.android.synthetic.main.setting_control_switch.view.*
import kotlinx.android.synthetic.main.setting_control_text.view.*


class SettingsGroup(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        val view = inflate(context, R.layout.setting_control_group, this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwitchSettingParent)
        val title = typedArray.getString(R.styleable.SwitchSettingParent_title)
        view.groupTitle.text = title
        typedArray.recycle()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        val contentView = this.findViewById<LinearLayout>(R.id.settingsGroupOptions)

        if (contentView == null) {
            // This is called once when the SettingsGroup is constructed. Apparently its adds itself?
            super.addView(child, index, params)
        } else {
            // Forward these calls to the content view
            if (contentView.childCount > 0) {
                contentView.addView(SettingsDivider(context))
            }
            contentView.addView(child, index, params)
        }
    }
}

class SwitchSettingItem(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    val layout = LayoutInflater.from(context).inflate(R.layout.setting_control_switch, this, true) as ConstraintLayout

    init {
        context.obtainStyledAttributes(attrs, R.styleable.SwitchSettingParent).use { typedArray ->
            val title = typedArray.getString(R.styleable.SwitchSettingParent_title)
            layout.switchControlText.text = title
        }

        layout.switchControlSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChanged(isChecked)
        }
    }

    var checked: Boolean = false
        set(value) {
            field = value
            layout.switchControlSwitch.isChecked = checked
        }

    var onSwitchChanged: (Boolean) -> Unit = {}
}

class TextSettingItem(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    val layout = LayoutInflater.from(context).inflate(R.layout.setting_control_text, this, true) as ConstraintLayout

    private var modalSuffix: String? = null
    private lateinit var title: String

    var onTextChanged: ((String) -> Unit)? = null
        set(value) {
            field = value

            // Not all text controls are editable. Hide the chevron from the UI if they're display-only.
            // Also remove or add the click handler
            if (value == null) {
                layout.controlChevron.visibility = View.GONE
                layout.setOnClickListener(null)
                // Have to disable sound effects manually because the click handler is still alive after it's set null I guess. Thanks, Google.
                layout.isSoundEffectsEnabled = false
            } else {
                layout.controlChevron.visibility = View.VISIBLE
                layout.isSoundEffectsEnabled = true
                layout.setOnClickListener {
                    showEditTextDialog(
                        // lol why does "findActivity" not return an ACTIVITY? So stupid
                        activity = findActivity(context) as Activity,
                        title = title,
                        suffix = modalSuffix,
                        yesAction = value
                    )
                }
            }
        }

    init {
        layout.controlChevron.visibility = View.GONE

        context.obtainStyledAttributes(attrs, R.styleable.SwitchSettingParent).use { typedArray ->
            title = typedArray.getString(R.styleable.SwitchSettingParent_title) ?: ""
            layout.textControlText.text = title
        }

        context.obtainStyledAttributes(attrs, R.styleable.SwitchSettingText).use { typedArray ->
            modalSuffix = typedArray.getString(R.styleable.SwitchSettingText_modalSuffix)
        }
    }

    var text: String = ""
        set(value) {
            field = value
            layout.controlValue.text = text
        }
}

class ListSelectSettingItem(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    val layout = LayoutInflater.from(context).inflate(R.layout.setting_control_text, this, true) as ConstraintLayout

    private lateinit var title: String

    init {
        context.obtainStyledAttributes(attrs, R.styleable.SwitchSettingParent).use { typedArray ->
            title = typedArray.getString(R.styleable.SwitchSettingParent_title) ?: ""
            layout.textControlText.text = title
        }

        layout.setOnClickListener {
            showListSelectDialog(
                activity = findActivity(context) as Activity,
                title = title,
                yesAction = onOptionPicked,
                options = options,
            )
        }
    }

    var text: String = ""
        set(value) {
            field = value
            layout.controlValue.text = text
        }

    var onOptionPicked: (String) -> Unit = {}

    var options: LinkedHashMap<String, Boolean> = linkedMapOf()
}

class SettingsDivider(context: Context) : View(context) {
    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.settingsDividerGrey))
        val height = getPixelsFromDp(1f)
        layoutParams = ViewGroup.MarginLayoutParams(ActionBar.LayoutParams.MATCH_PARENT, height).apply {
            setMargins(getPixelsFromDp(12f), 0, 0, 0)
        }
    }
}
