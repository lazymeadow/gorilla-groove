package com.gorilla.gorillagroove.ui.multiselectlist

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.DbUser
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.MainActivity
import com.gorilla.gorillagroove.ui.menu.*
import com.gorilla.gorillagroove.util.GGToast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_track_list.*
import kotlinx.android.synthetic.main.multiselect_list.view.*
import kotlinx.android.synthetic.main.multiselect_list_item.view.*
import kotlinx.coroutines.*
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit
import java.util.*


class UserItemViewHolder(itemView: View, checkedIds: MutableSet<Long>) : MultiselectList.MultiselectItemViewHolder<DbUser>(itemView, checkedIds) {
    override fun getRowName(datum: DbUser) = datum.name
}

class UserRecommendList(context: Context, attrs: AttributeSet? = null) : MultiselectList<DbUser>(context, attrs) {
    private var showAllUsers = false

    override fun loadData(): List<DbUser> {
        return GorillaDatabase.userDao.getOtherUsers()
            // Do not show users that haven't logged in for the last ~3 months unless we really want to see them
            .filter { user ->
                if (showAllUsers) {
                    return@filter true
                }

                val lastLogin = user.lastLogin ?: Instant.MIN
                return@filter ChronoUnit.DAYS.between(lastLogin, now()) < 90
            }
    }

    override fun createMyViewHolder(itemView: View, checkedIds: MutableSet<Long>): MultiselectItemViewHolder<DbUser> {
        return UserItemViewHolder(itemView, checkedIds)
    }

    override suspend fun initialize(activity: MainActivity, tracks: List<DbTrack>) = withContext(Dispatchers.Main) {
        showAllUsers = false

        super.initialize(activity, tracks)

        addButton.setOnClickListener {
            recommend()
        }

        addButton.text = "Send"
        filterOption.isVisible = true
        selectionTitle.text = "Recommend"

        filterOption.setOnClickListener {
            popoutMenu.toggleVisibility(ignoreIfRecent = true)
        }

        popoutMenu.setMenuList(
            listOf(
                CheckedMenuOption(title = "Show Inactive", showAllUsers) {
                    showAllUsers = it.isChecked
                    reload()
                },
            )
        )
    }

    private fun recommend() {
        logInfo("User tapped 'Send' button")

        if (checkedDataIds.isEmpty()) {
            logInfo("No users were chosen")
            return
        }

        val request = RecommendTrackRequest(targetUserIds = checkedDataIds.toList(), trackIds = tracksToAdd)

        val plurality = if (tracksToAdd.size == 1) "track" else "tracks"

        addButton.isVisible = false
        rightLoadingIndicator.isVisible = true

        GlobalScope.launch(Dispatchers.IO) {
            logInfo("Recommending tracks to users: $request")

            try {
                Network.api.recommendTracks(request)
            } catch (e: Throwable) {
                logError("Failed to recommend $plurality!", e)

                GGToast.show("Failed to recommend $plurality")

                withContext(Dispatchers.Main) {
                    addButton.isVisible = true
                    rightLoadingIndicator.isVisible = false
                }

                return@launch
            }

            logInfo("Tracks were recommended successfully")

            GGToast.show("Successfully recommended $plurality")

            withContext(Dispatchers.Main) {
                close()
            }
        }
    }
}

data class RecommendTrackRequest(
    val trackIds: List<Long>,
    val targetUserIds: List<Long>,
)
