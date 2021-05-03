package com.gorilla.gorillagroove.ui.reviewqueue

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.di.Network
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


class AddReviewSourceFragment : GGFragment(R.layout.fragment_add_review_source) {

    private lateinit var sourceListAdapter: SuggestionsListAdapter

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

        requireActivity().title_tv.text = if (mode == AddSourceMode.SPOTIFY_ARTIST) "Add Artist Queue" else "Add YouTube Queue"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Edit Review Sources view")

        if (mode == AddSourceMode.SPOTIFY_ARTIST) {
            fieldInput.hint = "Spotify Artist"
            fieldSubtext.hint = "Uploads to Spotify will be added to your review queue"
        } else {
            fieldInput.hint = "Channel Name or URL"
            fieldSubtext.hint = "Videos over 10 minutes will not be added to your queue"
        }

        fieldInput.addDebounceTextListener(
            lifecycleScope,
            onDebounceStart = {
                autocompleteSuggestions = emptyList()
            },
            onDebounceEnd = { newValue ->
                // In case somebody searches before the autocomplete finishes, we don't want it popping up
                if (addSourceIndicator.visibility == View.VISIBLE) {
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
                        if (mode == AddSourceMode.SPOTIFY_ARTIST) {
                            Network.api.getSpotifyAutocompleteResult(newValue).suggestions
                        } else {
                            Network.api.getYouTubeAutocompleteResult(newValue).suggestions
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

                lifecycleScope.launch { addSource() }
                true
            } else {
                false
            }
        }

        submitButton.setOnClickListener {
            view.hideKeyboard()
            lifecycleScope.launch { addSource() }
        }

        setHasOptionsMenu(true)

        setupRecyclerView()

        fieldInput.focusAndShowKeyboard()
    }

    private fun setupRecyclerView() = suggestionsList.apply {
        sourceListAdapter = SuggestionsListAdapter()
        adapter = sourceListAdapter

        val divider = createDivider(context)
        divider.setDrawable(ContextCompat.getDrawable(context, R.drawable.dividing_line)!!)
        addItemDecoration(divider)

        layoutManager = LinearLayoutManager(requireContext())
    }

    private suspend fun addSource() = withContext(Dispatchers.IO) {
        val sourceName = fieldInput.text?.toString() ?: ""

        if (sourceName.isEmpty()) {
            return@withContext
        }

        withContext(Dispatchers.Main) {
            submitButton.isEnabled = false
            addSourceIndicator.visibility = View.VISIBLE
        }

        val response = try {
            if (mode == AddSourceMode.SPOTIFY_ARTIST) {
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

            withContext(Dispatchers.Main) {
                submitButton.isEnabled = true
                addSourceIndicator.visibility = View.GONE
            }

            return@withContext
        }

        val newSource = response.asReviewSource()

        GorillaDatabase.reviewSourceDao.save(newSource)

        GGToast.show("$sourceName queue added")

        withContext(Dispatchers.Main) {
            submitButton.isEnabled = true
            addSourceIndicator.visibility = View.GONE
            requireActivity().onBackPressed()
        }
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
    SPOTIFY_ARTIST, YOUTUBE_CHANNEL
}

data class AutocompleteResult(val suggestions: List<String>)

data class AddArtistSourceRequest(val artistName: String)
data class AddYoutubeChannelRequest(val channelUrl: String?, val channelTitle: String?)
