// IDK. Android's style stuff seems bad. You can't have the same style name with two different components.
// So I made them use the same component, which then obviously couldn't have the same name for both components.
// I have no idea how you get styled attributes that are top level in the attrs.xml file. Surely this is possible,
// and would probably be a better solution, but nobody on Google has the answer and I'm sick of this.
@file:SuppressLint("CustomViewStyleable")

package com.gorilla.gorillagroove.ui.settings

import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.databinding.*
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.util.getPixelsFromDp
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
    init {
        val view = inflate(context, R.layout.setting_control_text, this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwitchSettingParent)
        val title = typedArray.getString(R.styleable.SwitchSettingParent_title)
        view.textControlText.text = title
        typedArray.recycle()
    }
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
