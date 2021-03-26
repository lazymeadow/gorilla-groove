package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.RequestManager
import com.gorilla.gorillagroove.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val TAG = "AppDebug"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "AppDebug : MainActivity"

    @Inject
    lateinit var glide: RequestManager

    private val viewModel: MainViewModel by viewModels()
    private val playerControlsViewModel: PlayerControlsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayShowTitleEnabled(false)


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // setupActionBarWithNavController(navController)
        bottomNavigationView.setupWithNavController(navController)

        navHostFragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.loginFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        playerControlView.visibility = View.GONE
                        title_tv.text = ""
                    }
                    R.id.mainFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "My Library"
                    }
                    R.id.playingFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Now Playing"
                    }
                    R.id.usersFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Users"
                    }
                    R.id.playlistsFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Playlists"
                    }
                    R.id.playlistFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                    }
                    R.id.settingsFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Settings"
                    }
                    R.id.trackPropertiesFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        playerControlView.visibility = View.GONE
                        title_tv.text = "Properties"
                    }
                    R.id.problemReportFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Problem Report"
                    }
                    else -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "var ar jag?"
                        supportActionBar?.displayOptions
                    }
                }
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
    }

    private fun initProgressBar() {
        audio_seek_bar.min = 0
        audio_seek_bar.max = 100
    }

    private fun subscribeObservers() {
        playerControlsViewModel.playbackState.observe(this, Observer {
            if (it.isPlaying) {
                playpause_button.setImageResource(R.drawable.ic_pause_24)
            } else {
                playpause_button.setImageResource(R.drawable.ic_play_arrow_24)
            }
        })

        playerControlsViewModel.repeatState.observe(this, Observer {
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

        playerControlsViewModel.isBuffering.observe(this, Observer {
            audio_seek_bar.isIndeterminate = it
        })

        playerControlsViewModel.currentTrackItem.observe(this, Observer { metadata ->
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

        playerControlsViewModel.mediaPosition.observe(this, Observer {
            track_position_textview.text = it.getSongTimeFromMilliseconds()
            audio_seek_bar.progress = it.toInt() / 1000
        })

        playerControlsViewModel.bufferPosition.observe(this, Observer {
            audio_seek_bar.secondaryProgress = it.toInt() / 1000
            //Log.d(TAG, "subscribeObservers: BUFFERED AMOUNT:  $it")
        })

    }
}

fun Long.getSongTimeFromMilliseconds(): String {
    return String.format(
        "%d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(this),
        TimeUnit.MILLISECONDS.toSeconds(this) % 60
    )
}