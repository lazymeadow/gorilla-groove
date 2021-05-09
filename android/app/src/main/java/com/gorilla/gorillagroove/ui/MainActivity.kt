package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.util.ShowAlertDialogRequest
import com.gorilla.gorillagroove.util.showAlertDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var mainRepository: MainRepository

    private val playerControlsViewModel: PlayerControlsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // We are using the AppTheme.Launcher theme to load the app. Swap back to the normal AppTheme now that we've loaded
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment

        setInitialFragment(navHostFragment)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navController = navHostFragment.navController

        bottomNavigationView.setupWithNavController(navController)

        navHostFragment.findNavController()
            // Apply sensible defaults when we navigate to a fragment. As fragments need to customize themselves further, they do when they are loaded
            .addOnDestinationChangedListener { _, _, _ ->
                multiselectIcon.visibility = View.GONE
                bottomNavigationView.visibility = View.VISIBLE
                playerControlView.visibility = View.VISIBLE
                toolbar.isVisible = true
            }

        subscribeObservers()
        initProgressBar()

        playpause_button.setOnClickListener {
            playerControlsViewModel.playPause()
        }

        repeat_button.setOnClickListener {
            playerControlsViewModel.repeat()
        }

        next_button.setOnClickListener {
            playerControlsViewModel.skipToNext()
        }

        previous_button.setOnClickListener {
            playerControlsViewModel.skipToPrevious()
        }

        audio_seek_bar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //Log.d(TAG, "onStartTrackingTouch: ")
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    playerControlsViewModel.seekTo(audio_seek_bar.progress.toLong() * 1000)
                }

            }
        )

        if (GGApplication.isUserSignedIn) {
            lifecycleScope.launch(Dispatchers.IO) {
                mainRepository.postDeviceVersion()
            }
        }
    }

    private fun setInitialFragment(navHostFragment: NavHostFragment) {
        val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph)

        graph.startDestination = getStartingFragmentId()

        navHostFragment.navController.graph = graph
    }

    fun getStartingFragmentId(): Int {
        return if (!GGApplication.isUserSignedIn) {
            R.id.loginFragment
        } else {
            val syncStatuses = runBlocking(Dispatchers.IO) {
                GorillaDatabase.syncStatusDao.findAll()
            }

            if (syncStatuses.isEmpty()) {
                R.id.firstTimeSyncFragment
            } else {
                R.id.libraryTrackFragment
            }
        }
    }

    private fun initProgressBar() {
        audio_seek_bar.min = 0
        audio_seek_bar.max = 100
    }

    private fun subscribeObservers() {
        lifecycleScope.launch(Dispatchers.Main) {
            playerControlsViewModel.playbackState.collect {
                if (it.isPlaying) {
                    playpause_button.setImageResource(R.drawable.ic_pause_24)
                } else {
                    playpause_button.setImageResource(R.drawable.ic_play_arrow_24)
                }
            }
        }

        playerControlsViewModel.repeatState.observe(this, {
            when (it) {
                REPEAT_MODE_NONE -> {
                    repeat_button.setImageResource(R.drawable.ic_repeat_24)
                    repeat_button.setColorFilter(ContextCompat.getColor(this, R.color.exo_white), android.graphics.PorterDuff.Mode.SRC_IN)
                }
                REPEAT_MODE_ONE -> {
                    repeat_button.setImageResource(R.drawable.ic_repeat_one_24)
                    repeat_button.setColorFilter(ContextCompat.getColor(this, R.color.ggSecondary), android.graphics.PorterDuff.Mode.SRC_IN)
                }
                REPEAT_MODE_ALL -> {
                    repeat_button.setImageResource(R.drawable.ic_repeat_24)
                    repeat_button.setColorFilter(ContextCompat.getColor(this, R.color.ggSecondary), android.graphics.PorterDuff.Mode.SRC_IN)
                }
                else -> {
                    //Log.d(TAG, "subscribeObservers: what is this? ${it}")
                }
            }
        })

        playerControlsViewModel.isBuffering.observe(this, {
            audio_seek_bar.isIndeterminate = it
        })

        playerControlsViewModel.currentTrackItem.observe(this, { metadata ->
            val artist = metadata.description?.subtitle?.takeIf { it.isNotBlank() }
            val name = metadata.description?.title?.takeIf { it.isNotBlank() }

            now_playing_textview.text = if (artist != null && name != null) {
                "$name - $artist"
            } else {
                name ?: artist
            }

            track_duration_textview.text = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).getSongTimeFromMilliseconds()
            audio_seek_bar.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt() / 1000
        })

        playerControlsViewModel.mediaPosition.observe(this, {
            track_position_textview.text = it.getSongTimeFromMilliseconds()
            audio_seek_bar.progress = it.toInt() / 1000
        })

        playerControlsViewModel.bufferPosition.observe(this, {
            audio_seek_bar.secondaryProgress = it.toInt() / 1000
        })
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            EventBus.getDefault().post(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    // For showing alert dialogs from a background action that has no concept of the current activity
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onModalOpenRequest(event: ShowAlertDialogRequest) {
        showAlertDialog(
            activity = this,
            title = event.title,
            message = event.message,
            yesText = event.yesText,
            noText = event.noText,
            yesAction = event.yesAction,
            noAction = event.noAction,
        )
    }

    override fun onBackPressed() {
        // If any fragment contained within this activity consumed the back press, don't do anything with it
        val handled = supportFragmentManager.fragments.handleBackPress()

        if (!handled) {
            super.onBackPressed()
        }
    }
}

private fun List<Fragment>.handleBackPress(): Boolean {
    return this.any { fragment ->
        when (fragment) {
            is NavHostFragment -> fragment.childFragmentManager.fragments.handleBackPress()
            is GGFragment -> fragment.onBackPressed()
            else -> false
        }
    }
}

fun Long.getSongTimeFromMilliseconds(): String {
    return String.format(
        "%d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(this),
        TimeUnit.MILLISECONDS.toSeconds(this) % 60
    )
}
