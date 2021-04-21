package com.gorilla.gorillagroove.ui.menu

import android.app.ActionBar
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.util.getPixelsFromDp
import kotlinx.android.synthetic.main.fragment_popout_item.view.*

class PopoutMenuItem(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.fragment_popout_item, this)
    }

    fun updateView(menuOption: MenuOption) {
        menuText.text = menuOption.title

        val iconResId = menuOption.iconResId
        if (iconResId == null) {
            menuIcon.setImageResource(0)
        } else {
            menuIcon.setImageResource(iconResId)
        }
    }
}

class PopoutMenuDivider(context: Context) : View(context) {
    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.foreground))
        val height = getPixelsFromDp(1f)
        layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, height)
    }
}
