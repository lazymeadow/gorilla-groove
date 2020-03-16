package com.example.gorillagroove.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.gorillagroove.R
import com.example.gorillagroove.activities.*
import com.example.gorillagroove.client.HttpClient
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.dto.TrackLinkResponse
import com.example.gorillagroove.utils.URLs
import com.example.gorillagroove.utils.logger
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

@RequiresApi(Build.VERSION_CODES.N)
private const val IMPORTANCE = NotificationManager.IMPORTANCE_LOW

private const val NOTIFICATION_CHANNEL_ID = "channel_id"
private const val CHANNEL_NAME = "Notification Channel"
private const val ACTION_PLAY = "com.example.gorillagroove.ACTION_PLAY"
private const val ACTION_PAUSE = "com.example.gorillagroove.ACTION_PAUSE"
private const val ACTION_NEXT = "com.example.gorillagroove.ACTION_NEXT"
private const val ACTION_PREVIOUS = "com.example.gorillagroove.ACTION_PREVIOUS"

private const val NOTIFY_ID = 1
private const val REQUEST_CODE_PLAY = 900
private const val REQUEST_CODE_PAUSE = 910
private const val REQUEST_CODE_NEXT = 920
private const val REQUEST_CODE_PREVIOUS = 930

class MusicPlayerService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener,
    CoroutineScope by MainScope() {

    // MediaPlayer is not thread safe, and because we are using it with asynchronous actions,
    // calls made to it need to be done in a synchronized block
    private val player = MediaPlayer()

    private val musicBind: IBinder = MusicBinder()
    private val httpClient = HttpClient()
    private val logger = logger()

    private var email = ""
    private var deviceId = ""
    private var songTitle = ""
    private var artist = ""
    private var shuffle = false
    private var hasAudioFocus = false
    private var paused = false
    private var playbackDelayed = false
    private var songPosition = 0
    private var lastRecordedTime = 0
    private var playCountPosition = 0
    private var playCountDuration = 0
    private var markedListened = false
    private var currentSongPosition = 0
    private var shuffledSongs: List<Int> = emptyList()

    private lateinit var userRepository: UserRepository
    private lateinit var songs: List<PlaylistSongDTO>
    private lateinit var audioManager: AudioManager
    private val mFocusLock = Object()

    override fun onCreate() {
        logger.debug("onCreate is called")
        super.onCreate()
        songPosition = 0
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initMusicPlayer()

        userRepository = UserRepository(GroovinDB.getDatabase().userRepository())
        if (deviceId.isBlank() || email.isBlank()) {
            launch {
                withContext(Dispatchers.IO) {
                    setUserInformation()
                }
            }
        }
    }

    private fun setUserInformation() {
        userRepository.lastLoggedInUser()?.let {
            deviceId = it.deviceId!!
            email = it.email
        }
    }

    private fun initMusicPlayer() {
        logger.debug("Initializing Media Player")
        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        player.setAudioStreamType(AudioManager.STREAM_MUSIC) // We are supporting API 19, and thus need this deprecated method
        player.setOnPreparedListener(this)
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    fun setSongList(songList: List<PlaylistSongDTO>) {
        songs = songList
    }

    fun setSong(songIndex: Int) {
        songPosition = songIndex
        currentSongPosition = songIndex
        if (shuffle) {
            songPosition = shuffledSongs.indexOf(songIndex)
        }
    }


    fun getPosition(): Int {
        synchronized(this) {
            val currentTime = System.currentTimeMillis().toInt()
            val elapsedPosition = currentTime - lastRecordedTime
            lastRecordedTime = currentTime
            playCountPosition += elapsedPosition // Milliseconds

            // Will come back to this once now playing is fixed
//        if (player.isPlaying) nowPlayingCounter += elapsedPosition
//        if (nowPlayingCounter >= 20000) {
//            sendNowPlayingRequest(deviceId, getCurrentTrackId())
//            nowPlayingCounter = 0
//        }
            if (!markedListened && playCountDuration > 0 && playCountPosition >= playCountDuration) {
                markListened(playCountPosition, player.currentPosition, playCountDuration, deviceId)
                markedListened = true
            }

            EventBus.getDefault()
                .post(UpdateSeekBarEvent("Sending Updated SeekBar Position", player.currentPosition))

            return player.currentPosition
        }
    }

    private fun getCurrentTrackId(): Long {
        return when (shuffle) {
            true -> songs[currentSongPosition].track.id
            false -> songs[songPosition].track.id
        }
    }

    fun getDuration(): Int {
        synchronized(this) {
            if (playCountDuration == 0) playCountDuration = (player.duration * 0.6).toInt()
            return player.duration
        }
    }

    fun isPlaying(): Boolean {
        synchronized(this) {
            return player.isPlaying
        }
    }

    fun pausePlayer() {
        synchronized(this) {
            player.pause()
        }
//        sendNowPlayingRequest(deviceId, null)
    }

    private fun start() {
        synchronized(this) {
            player.start()
            lastRecordedTime = System.currentTimeMillis().toInt()
        }
//        sendNowPlayingRequest(deviceId, getCurrentTrackId())
    }

    fun requestAudioFocus() {
        val requestAudioFocus: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mPlaybackAttributes = AudioAttributes.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
                .setOnAudioFocusChangeListener(this).build()
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        synchronized(mFocusLock) {
            when (requestAudioFocus) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    playbackDelayed = false
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    playbackDelayed = false
                    start()
                    EventBus.getDefault()
                        .post(
                            MediaPlayerLoadedEvent(
                                "Media Player Loaded, now Showing",
                                songTitle,
                                artist,
                                getDuration()
                            )
                        )
                }
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    playbackDelayed = true
                }
            }
        }
    }

    fun seek(position: Int) {
        synchronized(this) {
            player.seekTo(position)
            lastRecordedTime = System.currentTimeMillis().toInt()
        }
    }

    fun getBufferPercentage(): Int {
        synchronized(this) {
            return (player.currentPosition * 100) / player.duration
        }
    }

    fun getAudioSessionId(): Int {
        synchronized(this) {
            return player.audioSessionId
        }
    }

    fun playPrevious() {
        songPosition -= 1
        if (songPosition < 0) songPosition = songs.size - 1

        playSong()
    }

    fun playNext() {
        songPosition += 1
        if (songPosition >= songs.size) songPosition = 0

        logger.debug("Current songPosition is now=$songPosition")
        playSong()
    }

    inner class MusicBinder : Binder() {

        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (!action.isNullOrBlank()) {
                when (action) {
                    ACTION_PLAY -> EventBus.getDefault().post(MediaPlayerStartSongEvent("Resuming Play"))
                    ACTION_PAUSE -> EventBus.getDefault().post(MediaPlayerPauseEvent("Pausing Media"))
                    ACTION_NEXT -> EventBus.getDefault().post(PlayNextSongEvent("Playing Next Song"))
                    ACTION_PREVIOUS -> EventBus.getDefault().post(PlayPreviousSongEvent("Playing Previous Song"))
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onPrepared(mp: MediaPlayer) {
        requestAudioFocus()

        val notificationIntent = Intent(
            applicationContext,
            PlaylistActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playSongIntent = Intent(ACTION_PLAY)
        val pendingPlaySongIntent = PendingIntent.getService(
            applicationContext,
            REQUEST_CODE_PLAY,
            playSongIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseSongIntent = Intent(ACTION_PAUSE)
        val pendingPauseSongIntent = PendingIntent.getService(
            applicationContext,
            REQUEST_CODE_PAUSE,
            pauseSongIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        val nextSongIntent = Intent(ACTION_NEXT)
        val pendingNextSongIntent = PendingIntent.getService(
            applicationContext,
            REQUEST_CODE_NEXT,
            nextSongIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val previousSongIntent = Intent(ACTION_PREVIOUS)
        val pendingPreviousSongIntent = PendingIntent.getService(
            applicationContext,
            REQUEST_CODE_PREVIOUS,
            previousSongIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, IMPORTANCE)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            val notificationCompat = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "pause", pendingPauseSongIntent)
                .addAction(android.R.drawable.ic_media_play, "play", pendingPlaySongIntent)
                .addAction(android.R.drawable.ic_media_next, "next", pendingNextSongIntent)
                .addAction(
                    android.R.drawable.ic_media_previous,
                    "previous",
                    pendingPreviousSongIntent
                )
                .setContentTitle("Gorilla Groove")
                .setContentText("$songTitle - $artist")
                .setSmallIcon(R.drawable.logo)
                .setTicker("$songTitle - $artist")
                .setOngoing(true)

            startForeground(NOTIFY_ID, notificationCompat.build())
        } else {
            val notificationCompat = Notification.Builder(applicationContext)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "pause", pendingPauseSongIntent)
                .addAction(android.R.drawable.ic_media_play, "play", pendingPlaySongIntent)
                .addAction(android.R.drawable.ic_media_next, "next", pendingNextSongIntent)
                .setContentTitle("Gorilla Groove")
                .setContentText("$songTitle - $artist")
                .setSmallIcon(R.drawable.logo)
                .setTicker("$songTitle - $artist")
                .setOngoing(true)

            startForeground(NOTIFY_ID, notificationCompat.build())
        }
        EventBus.getDefault().post(
            MediaPlayerLoadedEvent(
                "Media Player Loaded, now Showing",
                songTitle,
                artist,
                getDuration()
            )
        )
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        clearPlayCountInfo()
        mp!!.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        EventBus.getDefault().post(PlayNextSongEvent("Resetting Music Player"))
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent != null) {
            if (intent.hasExtra("email")) email = intent.getStringExtra("email")
            if (intent.hasExtra("deviceId")) deviceId = intent.getStringExtra("deviceId")
        }
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        player.stop()
        player.release()
        return false
    }

    override fun onDestroy() {
        stopForeground(true)
    }

    fun setShuffle(): Boolean {
        shuffle = !shuffle
        if (shuffle) {
            shuffledSongs = songs.indices.toList().shuffled()
            logger.info(
                "Shuffling",
                "Shuffled list is: $shuffledSongs\n Songs list is ${songs.indices.toList()}"
            )
        } else {
            songPosition = currentSongPosition
        }
        logger.info("Shuffle Alert!", "Shuffle is now set to $shuffle")
        return shuffle
    }

    fun playSong() {
        synchronized(this) {
            if (player.isPlaying) {
                player.pause()
            }
        }

        clearPlayCountInfo()
        val song = if (shuffle) {
            currentSongPosition = shuffledSongs[songPosition]
            songs[currentSongPosition]
        } else {
            songs[songPosition]
        }
        songTitle = song.track.name.toString()
        artist = song.track.artist.toString()

        logger.debug("Getting track links for track: ${song.track.id}")

        httpClient.get(URLs.TRACK + song.track.id, TrackLinkResponse::class) { response ->
            if (response.success) {
                synchronized(this) {
                    player.reset()
                    player.setDataSource(response.data.songLink)
                    player.prepareAsync()
                }
            }
        }
    }

    private fun clearPlayCountInfo() {
        playCountPosition = 0
        playCountDuration = 0
        markedListened = false
    }

    // Will come back to this in a future update
//    private fun sendNowPlayingRequest(deviceId: String, trackId: Long?) {
//        Log.d("MusicPlayerService", "Sending \"Now Playing\" track=$trackId")
//        launch {
//            withContext(Dispatchers.IO) {
//                markListenedRequest(
//                    URLs.NOW_PLAYING,
//                    trackId,
//                    token,
//                    deviceId
//                )
//            }
//        }
//    }

    private fun markListened(
        playCountPosition: Int,
        playerCurrentPosition: Int,
        playCountDuration: Int,
        deviceId: String
    ) {
        val trackId = getCurrentTrackId()
        logger.debug("""
          |Marking track=$trackId as listened with:
          |  playCountPosition=$playCountPosition
          |  playerCurrentPosition=$playerCurrentPosition
          |  playCountDuration=$playCountDuration
          |""".trimMargin())

        val request = MarkListenedRequest(trackId, deviceId)
        httpClient.post(URLs.MARK_LISTENED, request)
    }

    data class MarkListenedRequest(val trackId: Long, val deviceId: String)

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                logger.debug("Audio Focus Gained")
                hasAudioFocus = true
                if (!paused) {
                    synchronized(this) {
                        player.start()
                    }
                    EventBus.getDefault()
                        .post(
                            MediaPlayerLoadedEvent(
                                "Media Player Loaded, now Showing",
                                songTitle,
                                artist,
                                getDuration()
                            )
                        )
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                logger.debug("Audio Focus Loss Transient")
                hasAudioFocus = false
                paused = true
                EventBus.getDefault()
                    .post(MediaPlayerTransientAudioLossEvent("Transient Audiofocus Loss, Pausing Playback"))
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                logger.debug("AudioFocusLoss")
                hasAudioFocus = false
                paused = true
                EventBus.getDefault()
                    .post(MediaPlayerAudioLossEvent("Audiofocus Loss, Stopping Playback"))
            }
        }
    }
}