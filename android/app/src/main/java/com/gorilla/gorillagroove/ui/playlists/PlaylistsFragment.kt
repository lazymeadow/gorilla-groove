package com.gorilla.gorillagroove.ui.playlists


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
import com.gorilla.gorillagroove.database.entity.DbPlaylist
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.ActionSheet
import com.gorilla.gorillagroove.ui.ActionSheetItem
import com.gorilla.gorillagroove.ui.ActionSheetType
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.util.GGToast
import com.gorilla.gorillagroove.util.showAlertDialog
import com.gorilla.gorillagroove.util.showEditTextDialog
import kotlinx.android.synthetic.main.fragment_playlists.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class PlaylistsFragment : Fragment(R.layout.fragment_playlists), PlaylistAdapter.OnPlaylistListener {

    private lateinit var playlistAdapter: PlaylistAdapter
    private var savedInstanceStateBundle: Bundle? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Playlists view")

        setHasOptionsMenu(true)
        savedInstanceStateBundle = savedInstanceState
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.Default) {
            val playlists = GorillaDatabase.playlistDao.findAll()

            withContext(Dispatchers.Main) {
                playlistAdapter.setPlaylists(playlists)
            }
        }
    }

    private fun setupRecyclerView() = playlists_key_rv.apply {
        playlistAdapter = PlaylistAdapter(this@PlaylistsFragment)
        addItemDecoration(createDivider(context))
        adapter = playlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onPlaylistClick(position: Int) {
        val playlist = playlistAdapter.playlists[position]
        val bundle = bundleOf("PLAYLIST" to playlist)

        findNavController().navigate(
            R.id.playlistTrackFragment,
            bundle
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.add_bar_menu, menu)
        menu.findItem(R.id.action_add_menu).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_add_menu -> {
                addPlaylist()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPlaylistLongClick(position: Int): Boolean {
        val playlist = playlistAdapter.playlists[position]

        showActionSheet(playlist)
        return true
    }

    private fun showActionSheet(playlist: DbPlaylist) {
        ActionSheet(
            requireActivity(),
            listOf(
                ActionSheetItem("Rename") {
                    renamePlaylist(playlist)
                },
                ActionSheetItem("Delete", ActionSheetType.DESTRUCTIVE) {
                    deletePlaylist(playlist)
                }
            )
        )
    }

    private fun renamePlaylist(playlist: DbPlaylist) {
        showEditTextDialog(
            activity = requireActivity(),
            title = "Rename '${playlist.name}'",
            yesAction = { newName ->
                lifecycleScope.launch(Dispatchers.IO) {
                    logInfo("Renaming playlist ${playlist.id} to $newName")
                    try {
                        Network.api.updatePlaylist(playlist.id, UpdatePlaylistRequest(newName))
                    } catch (e: Throwable) {
                        logError("Failed to rename playlist!", e)
                        GGToast.show("Playlist rename failed")

                        return@launch
                    }

                    logInfo("Playlist was renamed on the API. Renaming locally")
                    playlist.name = newName
                    GorillaDatabase.playlistDao.save(playlist)

                    withContext(Dispatchers.Main) {
                        playlistAdapter.setPlaylists(playlistAdapter.playlists)
                        playlistAdapter.notifyDataSetChanged()
                    }
                }
            }
        )
    }

    private fun addPlaylist() {
        showEditTextDialog(
            activity = requireActivity(),
            title = "New Playlist Name",
            yesAction = { name ->
                lifecycleScope.launch(Dispatchers.IO) {
                    logInfo("Creating playlist with name $name")
                    val playlistResponse = try {
                        Network.api.createPlaylist(UpdatePlaylistRequest(name))
                    } catch (e: Throwable) {
                        logError("Failed to create playlist!", e)
                        GGToast.show("Playlist create failed")

                        return@launch
                    }

                    logInfo("Playlist was created on the API. Saving locally")

                    val newPlaylist = playlistResponse.asPlaylist()
                    GorillaDatabase.playlistDao.save(newPlaylist)

                    withContext(Dispatchers.Main) {
                        playlistAdapter.setPlaylists(playlistAdapter.playlists + newPlaylist)
                        playlistAdapter.notifyDataSetChanged()
                    }
                }
            }
        )
    }

    private fun deletePlaylist(playlist: DbPlaylist) {
        showAlertDialog(
            activity = requireActivity(),
            title = "Delete '${playlist.name}'?",
            yesText = "Delete",
            noText = "Cancel",
            yesAction = {
                lifecycleScope.launch(Dispatchers.IO) {
                    logInfo("Deleting playlist ${playlist.id}")
                    try {
                        Network.api.deletePlaylist(playlist.id)
                    } catch (e: Throwable) {
                        logError("Failed to delete playlist!", e)
                        GGToast.show("Playlist delete failed")

                        return@launch
                    }

                    logInfo("Playlist was deleted on the API. Deleting locally")

                    GorillaDatabase.playlistDao.delete(playlist.id)

                    withContext(Dispatchers.Main) {
                        playlistAdapter.setPlaylists(playlistAdapter.playlists.filterNot { it.id == playlist.id })
                        playlistAdapter.notifyDataSetChanged()
                    }
                }
            }
        )
    }
}

data class UpdatePlaylistRequest(val name: String)

