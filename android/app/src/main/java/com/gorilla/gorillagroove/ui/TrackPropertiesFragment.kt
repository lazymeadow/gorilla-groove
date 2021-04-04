package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_track_properties.*


@AndroidEntryPoint
class TrackPropertiesFragment : Fragment(R.layout.fragment_track_properties) {

    private lateinit var track: DbTrack

    var newName: String? = null
    var newArtist: String? = null
    var newFeaturing: String? = null
    var newAlbum: String? = null
    var newGenre: String? = null
    var newTrackNum: Int? = null
    var newYear: Int? = null
    var newNote: String? = null

    var hasChanged: Boolean = false

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        track = arguments?.getSerializable("KEY_TRACK") as? DbTrack ?: run {
            throw IllegalArgumentException("KEY_TRACK was not supplied to TrackPropertiesFragment!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading TrackProperties view with track ID: ${track.id}")

        populateFragmentText()
        listenForChanges()
    }

    override fun onPause() {
        hideKeyboard(requireActivity())
        super.onPause()
    }

    private fun populateFragmentText() {
        et_name.setText(track.name)
        et_artist.setText(track.artist)
        et_featuring.setText(track.featuring)
        et_album.setText(track.album)
        et_genre.setText(track.genre ?: "")
        et_track_number.setText(track.trackNumber?.toString() ?: "")
        et_year.setText(track.releaseYear?.toString() ?: "")
        et_note.setText(track.note ?: "")
    }

    private fun listenForChanges() {
        et_name.doOnTextChanged { text, _, _, _ -> newName = text.toString() }
        et_artist.doOnTextChanged { text, _, _, _ -> newArtist = text.toString() }
        et_featuring.doOnTextChanged { text, _, _, _ -> newFeaturing = text.toString() }
        et_album.doOnTextChanged { text, _, _, _ -> newAlbum = text.toString() }
        et_genre.doOnTextChanged { text, _, _, _ -> newGenre = text.toString() }
        et_track_number.doOnTextChanged { text, _, _, _ -> newTrackNum = text.toString().toIntOrNull() }
        et_year.doOnTextChanged { text, _, _, _ -> newYear = text.toString().toIntOrNull() }
        et_note.doOnTextChanged { text, _, _, _ -> newNote = text.toString() }
    }

    private fun update() {
        val tu = TrackUpdate(
            trackIds = listOf(track.id),
            name = if (track.name != newName) newName.also { hasChanged = true } else null,
            artist = if (track.artist != newArtist) newArtist.also { hasChanged = true } else null,
            featuring = if (track.featuring != newFeaturing) newFeaturing.also { hasChanged = true } else null,
            album = if (track.album != newAlbum) newAlbum.also { hasChanged = true } else null,
            trackNumber = if (track.trackNumber != newTrackNum) newTrackNum.also { hasChanged = true } else null,
            genre = if (track.genre != newGenre) newGenre.also { hasChanged = true } else null,
            releaseYear = if (track.releaseYear != newYear) newYear.also { hasChanged = true } else null,
            note = if (track.note != newNote) newNote.also { hasChanged = true } else null,
            hidden = null,
            albumArtUrl = null,
            cropArtToSquare = null
        )
        if (hasChanged) {
//                viewModel.setUpdateEvent(UpdateEvent.UpdateTrack(tu))
        } else {
            Toast.makeText(requireContext(), "No changes found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_edit_properties_menu, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cancel_action -> {
                requireActivity().onBackPressed()
            }

            R.id.save_action -> {
                update()
            }
        }
        return true
    }
}
