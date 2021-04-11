package com.gorilla.gorillagroove.ui.menu

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.gorilla.gorillagroove.R
import kotlinx.android.synthetic.main.fragment_popout_menu.view.*


class PopoutMenu(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.fragment_popout_menu, this)

        alpha = 0.0f
    }

    lateinit var menuItems: List<MenuItem>
    private var lastVisibilityChange = System.currentTimeMillis()

    fun toggleVisibility(ignoreIfRecent: Boolean = false) {
        if (ignoreIfRecent && System.currentTimeMillis() - lastVisibilityChange < 200) {
            return
        }

        this.clearAnimation()

        val self = this
        if (this.visibility == View.GONE) {
            this.visibility = View.VISIBLE
            this.animate().alpha(1.0f).duration = 200
        } else {
            this.animate().alpha(0.0f).setDuration(200).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)

                    // This listener sticks around for whatever reason. Without this check, this will
                    // trigger the next time we make it VISIBLE, and it will instantly set it GONE again. Dumb.
                    if (self.alpha == 0.0f) {
                        self.visibility = View.GONE
                    }
                }
            })
        }
        lastVisibilityChange = System.currentTimeMillis()
    }

    fun setMenuList(menuItems: List<MenuItem>) {
        this.menuItems = menuItems
        menuItemContainer.removeAllViews()

        menuItems.forEach { menuItem ->
            when (menuItem) {
                is MenuOption -> {
                    val newView = PopoutMenuItem(context).apply {
                        updateView(menuItem)
                    }
                    newView.setOnClickListener {
                        this.toggleVisibility()

                        // If this is a sort option, clear out all other sort options before we invoke the item's click handler
                        if (menuItem is SortMenuOption) {
                            val existingSort = menuItem.sortDirection
                            clearSortOptions()
                            menuItem.sortDirection = existingSort
                        }

                        menuItem.clickHandler()
                    }
                    menuItem.view = newView
                    this.menuItemContainer.addView(newView)
                }
                is MenuDivider -> {
                    val newView = PopoutMenuDivider(context)
                    this.menuItemContainer.addView(newView)
                }
            }
        }
    }

    private fun clearSortOptions() {
        menuItems.forEach { item ->
            if (item is SortMenuOption) {
                item.sortDirection = SortDirection.NONE
                item.view.updateView(item)
            }
        }
    }
}

class CheckedMenuOption(
    title: String,
    var isChecked: Boolean,
    private val onClick: (CheckedMenuOption) -> Unit,
) : MenuOption(
    title = title,
    iconResId = if (isChecked) R.drawable.exo_ic_check else null,
) {
    override fun clickHandler() {
        isChecked = !isChecked
        iconResId = if (isChecked) R.drawable.exo_ic_check else null

        view.updateView(this)
        onClick(this)
    }
}

class SortMenuOption(
    title: String,
    sortDirection: SortDirection = SortDirection.NONE,
    val initialSortOnTap: SortDirection = SortDirection.ASC,
    private val onClick: (SortMenuOption) -> Unit,
) : MenuOption(
    title,
    iconResId = getIconFromSortDirection(sortDirection),
) {
    var sortDirection = sortDirection
    set(value) {
        field = value
        iconResId = getIconFromSortDirection(sortDirection)
    }

    override fun clickHandler() {
        sortDirection = when (sortDirection) {
            SortDirection.ASC -> SortDirection.DESC
            SortDirection.DESC -> SortDirection.ASC
            SortDirection.NONE -> initialSortOnTap
        }

        view.updateView(this)

        onClick(this)
    }
}

private fun getIconFromSortDirection(sortDirection: SortDirection) = when (sortDirection) {
    SortDirection.ASC -> R.drawable.ic_arrow_upward_black_24dp
    SortDirection.DESC -> R.drawable.ic_arrow_downward_black_24dp
    SortDirection.NONE -> null
}

enum class SortDirection { ASC, DESC, NONE }

abstract class MenuOption(
    val title: String,
    var iconResId: Int? = null,
) : MenuItem {
    abstract fun clickHandler()

    lateinit var view: PopoutMenuItem
}

class MenuDivider : MenuItem

interface MenuItem
