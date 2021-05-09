package com.gorilla.gorillagroove.ui.users

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.ui.menu.CheckedMenuOption
import kotlinx.android.synthetic.main.fragment_track_list.*
import kotlinx.android.synthetic.main.fragment_users.*
import kotlinx.android.synthetic.main.multiselect_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class UsersFragment : Fragment(R.layout.fragment_users), UserAdapter.OnUserListener {

    private lateinit var userAdapter: UserAdapter

    private lateinit var filterOption: MenuItem

    private var showAllUsers = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Users view")

        setHasOptionsMenu(true)
        setupRecyclerView()

        view.popoutMenu.setMenuList(
            listOf(
                CheckedMenuOption(title = "Show Inactive", showAllUsers) {
                    showAllUsers = it.isChecked

                    reload()
                },
            )
        )
    }

    override fun onStart() {
        super.onStart()

        reload()
    }

    private fun reload() {
        lifecycleScope.launch(Dispatchers.Default) {
            val users = GorillaDatabase.userDao.getOtherUsers()
                // Do not show users that haven't logged in for the last ~3 months unless we really want to see them
                .filter { user ->
                    if (showAllUsers) {
                        return@filter true
                    }

                    val lastLogin = user.lastLogin ?: Instant.MIN
                    return@filter ChronoUnit.DAYS.between(lastLogin, Instant.now()) < 90
                }

            withContext(Dispatchers.Main) {
                userAdapter.submitList(users)
            }
        }
    }

    private fun setupRecyclerView() = users_rv.apply {
        userAdapter = UserAdapter(this@UsersFragment)
        addItemDecoration(createDivider(context))
        adapter = userAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onUserClick(position: Int) {
        val user = userAdapter.userList[position]

        val bundle = bundleOf("USER" to user)

        findNavController().navigate(R.id.userTrackFragment, bundle)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        filterOption = menu.findItem(R.id.action_filter_menu)
        menu.findItem(R.id.action_search).isVisible = false
        filterOption.isVisible = true

        filterOption.setOnMenuItemClickListener {
            view?.popoutMenu?.toggleVisibility(ignoreIfRecent = true)
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_filter_menu -> {
                view?.popoutMenu?.toggleVisibility(ignoreIfRecent = true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
