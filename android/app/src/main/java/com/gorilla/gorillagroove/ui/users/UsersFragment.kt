package com.gorilla.gorillagroove.ui.users

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.dao.UserDao
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.createDivider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_users.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class UsersFragment : Fragment(R.layout.fragment_users), UserAdapter.OnUserListener {

    lateinit var userAdapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Users view")

        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.Default) {
            val users = GorillaDatabase.userDao.findAll()

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
        //Log.d(TAG, "onUserClick: clicked ${userAdapter.userList[position].username}")
    }

    override fun onUserLongClick(position: Int): Boolean {
        //Log.d(TAG, "onUserLongClick: long clicked ${userAdapter.userList[position].username}")
        return true
    }
}
