package com.gorilla.gorillagroove.ui.menu

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.dao.TrackSortType
import com.gorilla.gorillagroove.ui.MainActivity
import com.gorilla.gorillagroove.util.containsMotionEvent
import kotlinx.android.synthetic.main.fragment_popout_menu.view.*


class PopoutMenu(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.fragment_popout_menu, this)

        alpha = 0.0f
    }

    lateinit var menuItems: List<MenuItem>
    private var lastVisibilityChange = System.currentTimeMillis()
    var onOptionTapped: (MenuItem) -> Unit = {}

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

                        onOptionTapped(menuItem)
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

    fun handleScreenTap(motionEvent: MotionEvent, containingFragment: Fragment) {
        if (this.visibility == View.GONE) {
            return
        }

        // If someone touched the menu, don't manage the event as the menu knows how to manage its own visibility
        if (this.containsMotionEvent(
                containingFragment.requireView(),
                containingFragment.requireActivity() as MainActivity,
                motionEvent
            )
        ) {
            return
        }

        // If we got this far then we touched something that wasn't the popout menu or the toolbar and we can close it right away
        this.toggleVisibility()
    }
}

class CheckedMenuOption(
    title: String,
    isChecked: Boolean,
    var onClick: (CheckedMenuOption) -> Unit = {},
) : MenuOption(
    title = title,
    iconResId = if (isChecked) R.drawable.exo_ic_check else null,
) {
    var isChecked = isChecked
        set(value) {
            // This setter is kind of expensive (it updates the view) so abort if no change happened
            if (field == value) { return }

            field = value
            iconResId = if (isChecked) R.drawable.exo_ic_check else null
            view.updateView(this)
        }

    override fun clickHandler() {
        isChecked = !isChecked

        onClick(this)
    }
}

class SortMenuOption(
    title: String,
    val sortType: TrackSortType, // Could be made generic if it's ever used on another screen
    sortDirection: SortDirection = SortDirection.NONE,
    val initialSortOnTap: SortDirection = SortDirection.ASC,
    private val onClick: (SortMenuOption) -> Unit = {},
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

enum class LibraryViewType { TRACK, ARTIST, ALBUM }
fun getNavigationOptions(view: View, libraryViewType: LibraryViewType): Array<CheckedMenuOption> {
    val trackOption = CheckedMenuOption(title = "View by Track", libraryViewType == LibraryViewType.TRACK)
    val artistOption = CheckedMenuOption(title = "View by Artist", libraryViewType == LibraryViewType.ARTIST)
    val albumOption = CheckedMenuOption(title = "View by Album", libraryViewType == LibraryViewType.ALBUM)

    trackOption.onClick = {
        artistOption.isChecked = false
        albumOption.isChecked = false
        val destination = view.findNavController().currentDestination
        if (destination!!.id != R.id.libraryTrackFragment) {
            view.findNavController().navigate(R.id.libraryTrackFragment)
        } else {
            trackOption.isChecked = true
        }
    }
    artistOption.onClick = {
        trackOption.isChecked = false
        albumOption.isChecked = false
        val destination = view.findNavController().currentDestination
        if (destination!!.id != R.id.artistsFragment) {
            view.findNavController().navigate(R.id.artistsFragment)
        } else {
            artistOption.isChecked = true
        }
    }
    albumOption.onClick = {
        trackOption.isChecked = false
        artistOption.isChecked = false
        val destination = view.findNavController().currentDestination
        if (destination!!.id != R.id.albumFragment) {
            view.findNavController().navigate(R.id.albumFragment)
        } else {
            albumOption.isChecked = true
        }
    }

    return arrayOf(trackOption, artistOption, albumOption)
}


