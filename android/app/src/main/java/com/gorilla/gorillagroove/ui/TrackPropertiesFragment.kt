package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.network.track.TrackUpdateRequest
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.TrackService
import com.gorilla.gorillagroove.ui.settings.toReadableByteString
import com.gorilla.gorillagroove.util.GGToast
import com.gorilla.gorillagroove.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_track_properties.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class TrackPropertiesFragment : Fragment(R.layout.fragment_track_properties) {

    private lateinit var track: DbTrack

    @Inject
    lateinit var mainRepository: MainRepository

    private var newName: String? = null
    private var newArtist: String? = null
    private var newFeaturing: String? = null
    private var newAlbum: String? = null
    private var newGenre: String? = null
    private var newTrackNum: Int? = null
    private var newYear: Int? = null
    private var newNote: String? = null
    private var newPrivate: Boolean = false
    private var newHidden: Boolean = false
    private var newOfflineAvailability = OfflineAvailabilityType.NORMAL

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        track = arguments?.getSerializable("KEY_TRACK") as? DbTrack ?: run {
            throw IllegalArgumentException("KEY_TRACK was not supplied to TrackPropertiesFragment!")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading TrackProperties view with track ID: ${track.id}")

        requireActivity().apply {
            bottomNavigationView.visibility = View.GONE
            playerControlView.visibility = View.GONE
            title_tv.text = "Properties"
        }

        listenForChanges()
        populateFragmentText()

        lifecycleScope.launch {
            TrackService.getAlbumArt(track)?.let { art ->
                albumArt.setImageBitmap(art)
            }
        }
    }

    override fun onPause() {
        requireActivity().hideKeyboard()
        super.onPause()
    }

    private var oldInputMode: Int? = null

    override fun onStart() {
        super.onStart()

        // We actually want the keyboard to resize this screen, unlike every other screen in the app (so far)
        // Save the old input type we were using so we can restore it when the fragment goes away.
        // Otherwise we will leak this undesirable keyboard behavior to other fragments.
        oldInputMode = activity?.window?.attributes?.softInputMode

        // This thing is deprecated but I'm using it anyway. Why? Their alternative they outline in the deprecation looks absolutely stupid.
        // They want you to set up a listener on the root to adjust stuff yourself. Like. No. What are you smoking, Google.
        // Furthermore, this soft input mode IS VALID IN XML??? If you set this in your AndroidManifest there is no deprecation warning.
        // So clearly it seems like it's supported, but not supported when set programmatically? That makes no sense
        @Suppress("DEPRECATION")
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onStop() {
        super.onStop()

        oldInputMode?.let { activity?.window?.setSoftInputMode(it) }
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
        hiddenCheckbox.isChecked = track.hidden
        privateCheckbox.isChecked = track.thePrivate
        fileSizeValue.text = (track.filesizeArt + track.filesizeAudio).toLong().toReadableByteString()
        isCachedValue.text = if (track.songCachedAt != null) "Yes" else "No"

        newOfflineAvailability = track.offlineAvailability
        updateOfflineAvailabilityViews()
    }

    private fun getViewForOfflineType(offlineAvailabilityType: OfflineAvailabilityType): TextView {
        return when (offlineAvailabilityType) {
            OfflineAvailabilityType.AVAILABLE_OFFLINE -> offlineAvailabilityAlways
            OfflineAvailabilityType.NORMAL -> offlineAvailabilityNormal
            OfflineAvailabilityType.ONLINE_ONLY -> offlineAvailabilityNever
            else -> throw IllegalStateException("Offline availability type $offlineAvailabilityType not handled")
        }
    }

    private fun updateOfflineAvailabilityViews() {
        listOf(offlineAvailabilityAlways, offlineAvailabilityNormal, offlineAvailabilityNever).forEach {
            it.setTextColor(GGApplication.application.getColor(R.color.foreground))
        }

        val availabilityView = getViewForOfflineType(newOfflineAvailability)
        availabilityView.setTextColor(GGApplication.application.getColor(R.color.ggPrimary))
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
        hiddenCheckbox.setOnCheckedChangeListener { _, isChecked -> newHidden = isChecked }
        privateCheckbox.setOnCheckedChangeListener { _, isChecked -> newPrivate = isChecked }

        listOf(OfflineAvailabilityType.AVAILABLE_OFFLINE, OfflineAvailabilityType.NORMAL, OfflineAvailabilityType.ONLINE_ONLY).forEach { type ->
            getViewForOfflineType(type).setOnClickListener {
                newOfflineAvailability = type
                updateOfflineAvailabilityViews()
            }
        }
    }

    private fun update() {
        // trackNumber and releaseYear need to be set to -1 to clear them out on the API, as null means "no change"
        val request = TrackUpdateRequest(
            trackIds = listOf(track.id),
            name = if (track.name != newName) newName else null,
            artist = if (track.artist != newArtist) newArtist else null,
            featuring = if (track.featuring != newFeaturing) newFeaturing else null,
            album = if (track.album != newAlbum) newAlbum else null,
            trackNumber = if (track.trackNumber != newTrackNum) newTrackNum ?: -1 else null,
            genre = if (track.genre != newGenre) newGenre else null,
            releaseYear = if (track.releaseYear != newYear) newYear ?: -1 else null,
            note = if (track.note != newNote) newNote else null,
            hidden = if (track.hidden != newHidden) newHidden else null,
            private = if (track.thePrivate != newPrivate) newPrivate else null,
            offlineAvailability = if (track.offlineAvailability != newOfflineAvailability) newOfflineAvailability else null,
            albumArtUrl = null,
            cropArtToSquare = null
        )

        lifecycleScope.launch(Dispatchers.IO) {
            logInfo("User is updating metadata of track: ${track.id}")

            val updatedTrack = mainRepository.updateTrack(request) ?: run {
                GGToast.show("Failed to update track")
                return@launch
            }

            logInfo("Finished updating metadata of track: ${track.id}")
            GorillaDatabase.trackDao.save(updatedTrack)

            withContext(Dispatchers.Main) { activity?.onBackPressed() }
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
