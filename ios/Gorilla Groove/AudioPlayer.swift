import AVFoundation
import Foundation
import MediaPlayer

class AudioPlayer : CachingPlayerItemDelegate {
    
    private static var player: AVPlayer = {
        // I ran into an exc_bad_access code=1 error once and this answer on SO suggested splitting up the initialization
        // from the declaration. I don't know why this would matter, but I have tried it and hopefully we don't see it again.
        // (I think this crash has happened several times before, very intermittently)
        // Because this is a static class I am trying this with a lazy initialization block instead of a constructor call.
        // https://stackoverflow.com/a/57829394/13175115
        AVPlayer()
    }()
    
    private static var registeredCallbacks: Array<(_ time: Double)->()> = []
    
    private static var lastSongPlayHeartbeatTime = 0.0;
    
    private static let audioPlayerCacheDelegate = AudioPlayerCacheDelegate()
    
    private static weak var reviewQueueController: ReviewQueueController? = nil
    
    static var isPaused: Bool {
        get {
            // Seems ridiculous that there isn't a built in helper for this
            return player.rate == 0.0
        }
    }
    
    static var rate: Float {
        get {
            return player.rate
        }
    }
    
    static var currentTime: Double {
        get {
            return AudioPlayer.player.currentTime().seconds
        }
    }
    
    static func registerReviewQueueController(_ vc: ReviewQueueController) {
        reviewQueueController = vc
    }
    
    private init() { }
    
    static func initialize() {
        initializeControlCenter()

        player.automaticallyWaitsToMinimizeStalling = false
        player.volume = 1.0

        let time = CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player.addPeriodicTimeObserver(forInterval: time, queue: .main) { time in
            if (time.timescale.magnitude != 1_000_000_000) {
                return
            }
            
            let timeInSeconds = NSNumber(value: UInt64(time.value)).decimalValue / NSNumber(value: time.timescale.magnitude).decimalValue

            // This used to work this way because there was no socket, so continually sending events to the API was how the API
            // knew you were connected. Now that we are using a socket, this might be entirely unnecessary. But I'm feeling a bit
            // paranoid today so I'm not removing it outright. This does affect battery of the device though, so I've limited it to
            // sending an event once every minute.
            if (CACurrentMediaTime() - lastSongPlayHeartbeatTime > 60.0) {
                sendPlayEvent(NowPlayingTracks.currentTrack)
            }
            
            registeredCallbacks.forEach { callback in
                callback(Double(truncating: timeInSeconds as NSNumber))
            }
        }
    }
    
    private static func initializeControlCenter() {
        let rcc = MPRemoteCommandCenter.shared()

        rcc.playCommand.addTarget { event in
            GGNavLog.info("User played from the lock screen")

            AudioPlayer.play()
            return .success
        }
        rcc.pauseCommand.addTarget { event in
            GGNavLog.info("User paused from the lock screen")

            AudioPlayer.pause()
            return .success
        }
        rcc.changePlaybackPositionCommand.addTarget { event in
            GGNavLog.info("User manually adjusted playback position from the lock screen")

            let requestedSongTime = (event as! MPChangePlaybackPositionCommandEvent).positionTime
            seekTo(requestedSongTime)

            registeredCallbacks.forEach { callback in
                callback(requestedSongTime)
            }
            return .success
        }
        rcc.nextTrackCommand.addTarget { event in
            GGNavLog.info("User tapped next track from the lock screen")

            NowPlayingTracks.playNext()
            return .success
        }
        rcc.previousTrackCommand.addTarget { event in
            GGNavLog.info("User tapped previous track from the lock screen")

            NowPlayingTracks.playPrevious()
            return .success
        }
        rcc.likeCommand.addTarget { event in
            GGNavLog.info("User liked the current song from the lock screen")

            if let vc = reviewQueueController {
                DispatchQueue.main.async {
                    vc.acceptFromLockScreen()
                }
            } else {
                GGLog.error("User liked the current song but no review queue controller was registered!")
            }
            
            return .success
        }
        rcc.dislikeCommand.addTarget { event in
            GGNavLog.info("User disliked the current song from the lock screen")
            
            if let vc = reviewQueueController {
                DispatchQueue.main.async {
                    vc.rejectFromLockScreen()
                }
            } else {
                GGLog.error("User disliked the current song but no review queue controller was registered!")
            }
            
            return .success
        }
        
        rcc.playCommand.isEnabled = true
        rcc.pauseCommand.isEnabled = true
        rcc.skipBackwardCommand.isEnabled = false
        rcc.skipForwardCommand.isEnabled = false
        rcc.nextTrackCommand.isEnabled = true
        rcc.previousTrackCommand.isEnabled = true
        rcc.changePlaybackPositionCommand.isEnabled = true
        rcc.likeCommand.isEnabled = false
        rcc.dislikeCommand.isEnabled = false
        
        // I noticed that if playback is paused (by say, unplugging your phone from aux), the notification area
        // would, for some weird reason, say that the time was 0 seconds. This fix doesn't seem ideal, but at least
        // it's better than it was. The progress bar can still jump around a bit to the correct position though.
        // Both of these events seem to be important to listen for in this regard.
        [AVAudioSession.interruptionNotification, AVAudioSession.routeChangeNotification].forEach { interrupt in
            NotificationCenter.default.addObserver(
                forName: interrupt,
                object: nil,
                queue: nil
            ) { _ in
                MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] = AudioPlayer.currentTime
            }
        }
    }
    
    static func seekTo(_ time: Double) {
        player.seek(to: CMTime(seconds: time, preferredTimescale: 1000))
        MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] = time
    }
    
    static func addTimeObserver(callback: @escaping (_ time: Double) -> Void) {
        registeredCallbacks.append(callback)
    }
    
    static func play() {
        MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] = 1.0
        player.play()
        
        sendPlayEvent(NowPlayingTracks.currentTrack)
    }
    
    static func pause() {
        // If you leave the app with the music paused, the notification player doesn't seem to get the updated time...
        MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] = AudioPlayer.currentTime
        MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] = 0.0
        
        player.pause()
        
        sendPlayEvent(nil)
    }
    
    static func stop() {
        player.pause()
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        player.replaceCurrentItem(with: nil)
        
        sendPlayEvent(nil)
    }
    
    private static func setRatingEnabled(_ enabled: Bool) {
        let rcc = MPRemoteCommandCenter.shared()
        
        rcc.likeCommand.isEnabled = enabled
        rcc.dislikeCommand.isEnabled = enabled
    }
    
    // This will cache the song to disk while streaming it
    static func playNewLink(_ link: String, track: Track, shouldCache: Bool) {
        let playerItem = SongCachingPlayerItem(url: URL(string: link)!, trackId: track.id, shouldCache: shouldCache)
        playerItem.delegate = audioPlayerCacheDelegate

        playPlayerItem(playerItem, track)
    }
    
    // This plays a song that is already downloaded
    static func playSongData(_ songData: Data, track: Track) {
        let playerItem = CachingPlayerItem(data: songData, mimeType: "audio/mp3", fileExtension: "mp3")
        playPlayerItem(playerItem, track)
    }
    
    static func playPlayerItem(_ playerItem: AVPlayerItem, _ track: Track) {
        player.replaceCurrentItem(with: playerItem)
        player.playImmediately(atRate: 1.0)
        MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] = 1.0

        setRatingEnabled(track.inReview)
        
        sendPlayEvent(NowPlayingTracks.currentTrack)
    }
    
    private static func sendPlayEvent(_ track: Track?) {
        let request = PlayEventRequest(trackId: track?.id, isPlaying: track != nil)
        
        if (track != nil) {
            lastSongPlayHeartbeatTime = CACurrentMediaTime()
            WebSocket.connect()
            WebSocket.sendMessage(request)
        } else {
            WebSocket.disconnect()
        }
    }
    
    struct PlayEventRequest: Encodable {
        let trackId: Int?
        let isPlaying: Bool?
        let messageType: String = "NOW_PLAYING"
    }
}

class AudioPlayerCacheDelegate : CachingPlayerItemDelegate {
    func playerItem(_ playerItem: CachingPlayerItem, didFinishDownloadingData data: Data) {
        let songCacheItem = playerItem as! SongCachingPlayerItem
        let trackId = songCacheItem.trackId
        
        if !songCacheItem.shouldCache {
            GGLog.debug("Song was cacheable, but was not told to cache. Not caching song data for Track \(trackId)")
            return
        }

        GGLog.info("Track \(trackId) is downloaded and ready for storing")

        CacheService.setCachedData(trackId: trackId, data: data, cacheType: .song)
        
        GGLog.info("Track \(trackId) is saved")
    }
    
    private var playbackStalledTrackId: Int?
    
    func playerItemReadyToPlay(_ playerItem: CachingPlayerItem) {
        // The description of CachingPlayerItem says this is called after pre-buffering. I believe I have something set up
        // to auto-play after that, so this might be pointless. But I am curious if it is invoked after playback stalls.
        let songCacheItem = playerItem as! SongCachingPlayerItem
        GGLog.debug("Player item ready to play for track ID \(songCacheItem.trackId)")
        
        // Doesn't matter if it was stalled or not. If the most recent stall was not for this track, don't worry about stalls anymore.
        // If a different track finished than the one that stalled, that would just be weird. Though maybe it could happen.
        if songCacheItem.trackId != playbackStalledTrackId {
            playbackStalledTrackId = nil
        }
        
        // Could be a number of things wrong with this. The struggle I have right now is that, even after a stalled item says
        // that it is "ready to play", just pushing "play" again does not cause it to play. Hopefully just resetting the item
        // reference here will make it work. Also a potential issue in that it could have audio start playing that the user
        // wanted to pause. Need to see how it works in practice. Might need a new Bool to remember a user's "intent" to be playing.
        if songCacheItem.trackId == playbackStalledTrackId {
            if let track = TrackDao.findById(songCacheItem.trackId) {
                AudioPlayer.playPlayerItem(songCacheItem, track)
            } else {
                GGLog.warning("Player item was ready to play a song that has been deleted")
            }
        }
    }
    
    func playerItemPlaybackStalled(_ playerItem: CachingPlayerItem) {
        let songCacheItem = playerItem as! SongCachingPlayerItem
        GGLog.warning("Player item playback was stalled for track ID \(songCacheItem.trackId)")
        playbackStalledTrackId = songCacheItem.trackId
    }
}

class SongCachingPlayerItem : CachingPlayerItem {
    let trackId: Int
    let shouldCache: Bool
    
    init(url: URL, trackId: Int, shouldCache: Bool) {
        self.trackId = trackId
        self.shouldCache = shouldCache

        super.init(url: url, customFileExtension: nil)
    }
}
