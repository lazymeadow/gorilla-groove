package com.gorilla.gorillagroove.ui.reviewqueue

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.BackgroundTaskService
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.ui.GGFragment
import com.gorilla.gorillagroove.ui.createDivider
import com.gorilla.gorillagroove.util.GGToast
import com.gorilla.gorillagroove.util.addDebounceTextListener
import com.gorilla.gorillagroove.util.focusAndShowKeyboard
import com.gorilla.gorillagroove.util.hideKeyboard
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_add_review_source.*
import kotlinx.android.synthetic.main.fragment_input_dialog.*
import kotlinx.android.synthetic.main.simple_text_info_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


class AddReviewSourceFragment : GGFragment(R.layout.fragment_add_review_source) {

    private lateinit var sourceListAdapter: SuggestionsListAdapter

    private val loading = MutableStateFlow(false)

    private var autocompleteSuggestions: List<String> = emptyList()
        set(value) {
            field = value

            lifecycleScope.launch(Dispatchers.Main) {
                recyclerViewBorder.visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
                sourceListAdapter.notifyDataSetChanged()

                autocompleteLoading.visibility = View.GONE
            }
        }

    private val mode: AddSourceMode by lazy {
        requireArguments().getSerializable("MODE") as AddSourceMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().title_tv.text = when (mode) {
            AddSourceMode.ADD_SPOTIFY_ARTIST -> "Add Artist Queue"
            AddSourceMode.ADD_YOUTUBE_CHANNEL -> "Add YouTube Queue"
            AddSourceMode.SEARCH_SPOTIFY_ARTIST -> "Search Spotify"
            AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO -> "Download from YouTube"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Edit Review Sources view")

        fieldInput.hint = when (mode) {
            AddSourceMode.ADD_SPOTIFY_ARTIST, AddSourceMode.SEARCH_SPOTIFY_ARTIST -> "Spotify Artist"
            AddSourceMode.ADD_YOUTUBE_CHANNEL -> "Channel Name or URL"
            AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO -> "YouTube Video URL"
        }

        fieldSubtext.text = when (mode) {
            AddSourceMode.ADD_SPOTIFY_ARTIST -> "Uploads to Spotify will be added to your review queue"
            AddSourceMode.SEARCH_SPOTIFY_ARTIST -> ""
            AddSourceMode.ADD_YOUTUBE_CHANNEL -> "Videos over 10 minutes will not be added to your queue"
            AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO -> "Playlist downloads can take up to a minute to start"
        }

        fieldInput.addDebounceTextListener(
            lifecycleScope,
            onDebounceStart = {
                autocompleteSuggestions = emptyList()
            },
            onDebounceEnd = { newValue ->
                // In case somebody searches before the autocomplete finishes, we don't want it popping up
                if (loading.value) {
                    return@addDebounceTextListener
                }

                // When we programmatically call setText(), we need a way to not have this callback happen.
                // We don't want the autocomplete search to happen unless it was user-supplied input
                if (!fieldInput.hasFocus()) {
                    return@addDebounceTextListener
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    autocompleteLoading.visibility = View.VISIBLE
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    autocompleteSuggestions = try {
                        when (mode) {
                            AddSourceMode.ADD_SPOTIFY_ARTIST, AddSourceMode.SEARCH_SPOTIFY_ARTIST -> {
                                Network.api.getSpotifyAutocompleteResult(newValue).suggestions
                            }
                            AddSourceMode.ADD_YOUTUBE_CHANNEL -> {
                                Network.api.getYouTubeAutocompleteResult(newValue).suggestions
                            }
                            AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO -> emptyList()
                        }
                    } catch (e: Throwable) {
                        logError("Failed to get autocomplete results!", e)
                        emptyList()
                    }
                }
            }
        )

        fieldInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                view.hideKeyboard()
                autocompleteSuggestions = emptyList()

                lifecycleScope.launch { submit() }
                true
            } else {
                false
            }
        }

        submitButton.setOnClickListener {
            view.hideKeyboard()
            lifecycleScope.launch { submit() }
        }

        submitButton.text = when(mode) {
            AddSourceMode.ADD_SPOTIFY_ARTIST, AddSourceMode.ADD_YOUTUBE_CHANNEL -> "Queue it up"
            AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO -> "Download"
            AddSourceMode.SEARCH_SPOTIFY_ARTIST -> "Search Artist"
        }

        setHasOptionsMenu(true)

        setupRecyclerView()

        fieldInput.focusAndShowKeyboard()

        lifecycleScope.launch(Dispatchers.Main) {
            loading.collect { newValue ->
                submitButton.isEnabled = !newValue
                addSourceIndicator.visibility = if (newValue) View.VISIBLE else View.GONE
            }
        }

        if (mode == AddSourceMode.SEARCH_SPOTIFY_ARTIST || mode == AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO) {
            lifecycleScope.launch(Dispatchers.Main) {
                BackgroundTaskService.totalDownloads.collect { setDownloadProgressText() }
                launch {
                    BackgroundTaskService.currentDownload.collect { setDownloadProgressText() }
                }
            }
        }
    }

    private fun setDownloadProgressText() {
        if (BackgroundTaskService.totalDownloads.value == 0) {
            downloadProgressText.visibility = View.GONE
        } else {
            downloadProgressText.visibility = View.VISIBLE

            downloadProgressText.text = "Currently downloading ${BackgroundTaskService.currentDownload.value + 1} of ${BackgroundTaskService.totalDownloads.value} songs." +
                    "\n\nYou may add more, or leave this screen. The downloading will continue"
        }
    }

    private fun setupRecyclerView() = suggestionsList.apply {
        sourceListAdapter = SuggestionsListAdapter()
        adapter = sourceListAdapter

        val divider = createDivider(context)
        divider.setDrawable(ContextCompat.getDrawable(context, R.drawable.dividing_line)!!)
        addItemDecoration(divider)

        layoutManager = LinearLayoutManager(requireContext())
    }

    private suspend fun submit() {
        val text = fieldInput.text?.toString() ?: ""

        if (text.isEmpty()) {
            return
        }

        when (mode) {
            AddSourceMode.ADD_SPOTIFY_ARTIST, AddSourceMode.ADD_YOUTUBE_CHANNEL -> addSource(text)
            AddSourceMode.DOWNLOAD_YOUTUBE_VIDEO -> downloadFromYoutube(text)
            AddSourceMode.SEARCH_SPOTIFY_ARTIST -> searchSpotify(text)
        }
    }

    private suspend fun addSource(sourceName: String) = withContext(Dispatchers.IO) {
        loading.value = true

        val response = try {
            if (mode == AddSourceMode.ADD_SPOTIFY_ARTIST) {
                val request = AddArtistSourceRequest(sourceName)
                Network.api.subscribeToSpotifyArtist(request)
            } else {
                val isUrl = sourceName.startsWith("https:")
                val request = AddYoutubeChannelRequest(
                    channelUrl = if (isUrl) sourceName else null,
                    channelTitle = if (isUrl) null else sourceName,
                )
                Network.api.subscribeToYoutubeChannel(request)
            }
        } catch (e: Throwable) {
            logError("Failed to create new $mode source!", e)

            GGToast.show("Failed to add queue")

            loading.value = false

            return@withContext
        }

        val newSource = response.asReviewSource()

        GorillaDatabase.reviewSourceDao.save(newSource)

        GGToast.show("$sourceName queue added")

        loading.value = false

        withContext(Dispatchers.Main) {
            requireActivity().onBackPressed()
        }
    }

    private suspend fun downloadFromYoutube(url: String) = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) {
            GGToast.show("URLs should begin with 'https://'")
            return@withContext
        }

        loading.value = true

        val taskResponse = try {
            Network.api.queueYoutubeBackgroundTask(DownloadYTVideoRequest(url))
        } catch (e: Throwable) {
            logError("Failed to enqueue YouTube download of $url!", e)
            return@withContext
        }

        BackgroundTaskService.addBackgroundTasks(taskResponse.items)

        GGToast.show("Your download has been started", Toast.LENGTH_LONG)

        loading.value = false

        withContext(Dispatchers.Main) {
            fieldInput.setText("")
        }
    }

    private suspend fun searchSpotify(artist: String) = withContext(Dispatchers.IO) {

    }

    inner class SuggestionsListAdapter : RecyclerView.Adapter<SuggestionsListAdapter.SuggestionsItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionsItemViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.simple_text_info_item, parent, false)
            return SuggestionsItemViewHolder(itemView)
        }

        override fun getItemCount() = min(autocompleteSuggestions.size, 5)

        override fun onBindViewHolder(holder: SuggestionsItemViewHolder, position: Int) {
            holder.setText(autocompleteSuggestions[position])
        }

        inner class SuggestionsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private lateinit var text: String

            init {
                itemView.setOnClickListener {
                    fieldInput.clearFocus()
                    fieldInput.setText(text)

                    autocompleteSuggestions = emptyList()
                }
            }

            fun setText(text: String) {
                this.text = text
                itemView.textItem.text = text
            }
        }
    }
}

enum class AddSourceMode {
    ADD_SPOTIFY_ARTIST, ADD_YOUTUBE_CHANNEL, SEARCH_SPOTIFY_ARTIST, DOWNLOAD_YOUTUBE_VIDEO
}

data class AutocompleteResult(val suggestions: List<String>)

data class AddArtistSourceRequest(val artistName: String)
data class DownloadYTVideoRequest(val url: String)
data class AddYoutubeChannelRequest(val channelUrl: String?, val channelTitle: String?)
