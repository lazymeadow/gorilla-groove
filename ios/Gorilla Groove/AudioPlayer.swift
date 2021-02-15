import AVFoundation
import Foundation
import MediaPlayer

class AudioPlayer : CachingPlayerItemDelegate {
    
    @SettingsBundleStorage(key: "offline_mode_automatic_enable_buffer")
    private static var enableOfflineModeAfterLongBuffer: Bool
    
    private static var player: AVPlayer = {
        // I ran into an exc_bad_access code=1 error once and this answer on SO suggested splitting up the initialization
        // from the declaration. I don't know why this would matter, but I have tried it and hopefully we don't see it again.
        // (I think this crash has happened several times before, very intermittently)
        // Because this is a static class I am trying this with a lazy initialization block instead of a constructor call.
        // https://stackoverflow.com/a/57829394/13175115
        AVPlayer()
    }()
    
    private static var registeredCallbacks: [(_ time: Double)->()] = []
    
    private static var lastSongPlayHeartbeatTime = 0.0;
    
    private static let audioPlayerCacheDelegate = AudioPlayerCacheDelegate()
    
    private static weak var reviewQueueController: ReviewQueueController? = nil
    
    private(set) static var isPlaybackWanted = false
    
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
    
    private static var lastTimeUpdateSeconds: Double = 0
    static var currentTime: Double {
        get {
            // You'd think that just returning the player's current time would be ok, but I ran into this function taking SIX SECONDS to invoke on the main thread!
            // I only saw it happen on the simulator so far, but it's still really annoying. So now I store the current time in a variable to avoid this.
//            return AudioPlayer.player.currentTime().seconds
            return lastTimeUpdateSeconds
        }
    }
    
    static func registerReviewQueueController(_ vc: ReviewQueueController) {
        reviewQueueController = vc
    }
    
    private static let timeControlStatusObserver = TimeControlStatusObserver()
    
    private init() { }
    
    static func initialize() {
        initializeControlCenter()

        player.automaticallyWaitsToMinimizeStalling = true
        player.volume = 1.0

        let time = CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player.addPeriodicTimeObserver(forInterval: time, queue: .main) { time in
            if (time.timescale.magnitude != 1_000_000_000) {
                return
            }
            
            let timeInSeconds = NSNumber(value: UInt64(time.value)).decimalValue / NSNumber(value: time.timescale.magnitude).decimalValue
            let doubleSeconds = Double(truncating: timeInSeconds as NSNumber)
            lastTimeUpdateSeconds = doubleSeconds
            
            // This used to work this way because there was no socket, so continually sending events to the API was how the API
            // knew you were connected. Now that we are using a socket, this might be entirely unnecessary. But I'm feeling a bit
            // paranoid today so I'm not removing it outright. This does affect battery of the device though, so I've limited it to
            // sending an event once every minute.
            if (CACurrentMediaTime() - lastSongPlayHeartbeatTime > 60.0) {
                sendPlayEvent(NowPlayingTracks.currentTrack)
            }
            
            registeredCallbacks.forEach { callback in
                callback(doubleSeconds)
            }
        }
        
        player.addObserver(timeControlStatusObserver, forKeyPath: "timeControlStatus", options: [.old, .new], context: nil)
    }
    
    private static var isStalled = false {
        didSet {
            stallCheckPending = false
        }
    }
    private static var stallCheckPending = false
    fileprivate static func onTimeControlStatusChange() {
        if player.timeControlStatus == .waitingToPlayAtSpecifiedRate {
            checkStall()
        } else if player.timeControlStatus == .playing {
            AudioPlayer.observers.values.forEach { $0(.PLAYING) }
            if isStalled {
                GGLog.info("Stalling recovered")
            }
            isStalled = false
        } else if player.timeControlStatus == .paused {
            AudioPlayer.observers.values.forEach { $0(.PAUSED) }
        }
    }
    
    private static func checkStall() {
        if !stallCheckPending {
            stallCheckPending = true
            
            // Give the app a chance to catch up before we notify everything that we're buffering.
            // It can take a second for the "playing" event to kick in, even if it's already available.
            DispatchQueue.global().asyncAfter(deadline: .now() + 1.5) {
                if player.timeControlStatus == .waitingToPlayAtSpecifiedRate && stallCheckPending {
                    isStalled = true
                    GGLog.warning("Playback was stalled")
                    
                    
                    AudioPlayer.observers.values.forEach { $0(.BUFFERING) }
                    
                    // If we're still stalled after a while, then enable offline mode. But only if the track didn't change. If the user
                    // is fucking with stuff don't interfere with them.
                    let stalledTrackId = NowPlayingTracks.currentTrack?.id
                    if stalledTrackId != nil && enableOfflineModeAfterLongBuffer && !OfflineStorageService.offlineModeEnabled {
                        DispatchQueue.global().asyncAfter(deadline: .now() + 4) {
                            if isStalled && !OfflineStorageService.offlineModeEnabled && stalledTrackId == NowPlayingTracks.currentTrack?.id && isPlaybackWanted {
                                
                                // I think that more could be done to make this process better. But for now, just make sure that anything in the now playing
                                // of the user are actually available offline. Otherwise we'd be putting them in offline mode for no real benefit to them.
                                let anySongsAvailableOffline = NowPlayingTracks.getNowPlayingTracks().contains(where: { $0.songCachedAt != nil })
                                
                                if anySongsAvailableOffline {
                                    // Temporarily mute the player in case the song comes back while we're doing text to speech as that would be annoying
                                    let oldVolume = player.volume
                                    player.volume = 0
                                    TextSpeaker.speak("Playing offline music") {
                                        OfflineStorageService.offlineModeEnabled = true
                                        player.volume = oldVolume
                                    }
                                    Toast.show("Offline mode automatically enabled")
                                    GGLog.warning("Offline mode was automatically enabled due to a long stall")
                                } else {
                                    GGLog.warning("User has enabled 'offline mode after stall' but no songs in their now playing were available offline. Not engaging it.")
                                }
                            }
                        }
                    }
                }
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
        
        rcc.likeCommand.localizedTitle = "Approve"
        rcc.dislikeCommand.localizedTitle = "Reject"
        rcc.likeCommand.localizedShortTitle = "Approve"
        rcc.dislikeCommand.localizedShortTitle = "Reject"

        // I noticed that if playback is paused (by say, unplugging your phone from aux), the notification area
        // would, for some weird reason, say that the time was 0 seconds. This fix doesn't seem ideal, but at least
        // it's better than it was. The progress bar can still jump around a bit to the correct position though.
        // Both of these events seem to be important to listen for in this regard.
        [AVAudioSession.interruptionNotification, AVAudioSession.routeChangeNotification].forEach { interrupt in
            GGLog.info("Playback was interrupted by the system with type: \(interrupt.rawValue)")
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
        player.play()
        isPlaybackWanted = true
        
        sendPlayEvent(NowPlayingTracks.currentTrack)
    }
    
    static func pause() {
        player.pause()
        isPlaybackWanted = false

        sendPlayEvent(nil)
    }
    
    static func stop() {
        player.pause()
        player.replaceCurrentItem(with: nil)
        isPlaybackWanted = false

        sendPlayEvent(nil)
        self.observers.values.forEach { $0(.STOPPED) }
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
        isStalled = false
        isPlaybackWanted = true
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(playerDidFinishPlayingItem),
            name: .AVPlayerItemDidPlayToEndTime,
            object: playerItem
        )
        
        lastTimeUpdateSeconds = 0
        player.replaceCurrentItem(with: playerItem)
        player.playImmediately(atRate: 1.0)
        
        setRatingEnabled(track.inReview)
        
        sendPlayEvent(NowPlayingTracks.currentTrack)
    }
    
    @objc private static func playerDidFinishPlayingItem() {
        GGLog.info("Finished playing current audio item")
        NowPlayingTracks.playNext()
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
    
    private static var observers = [UUID : (PlaybackStateType) -> Void]()
    
    @discardableResult
    static func observePlaybackChanged<T: AnyObject>(
        _ observer: T,
        closure: @escaping (T, PlaybackStateType) -> Void
    ) -> ObservationToken {
        let id = UUID()
        
        observers[id] = { [weak observer] playbackState in
            guard let observer = observer else {
                observers.removeValue(forKey: id)
                return
            }

            closure(observer, playbackState)
        }
        
        return ObservationToken {
            observers.removeValue(forKey: id)
        }
    }
}

enum PlaybackStateType {
    case PLAYING
    case PAUSED
    case STOPPED
    case BUFFERING
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
    }
    
    private var playbackStalledTrackId: Int?
    
    // This doesn't seem to get called after an item stalls, so it's of limited use
    func playerItemReadyToPlay(_ playerItem: CachingPlayerItem) {
        let songCacheItem = playerItem as! SongCachingPlayerItem
        GGLog.debug("Player item ready to play for track ID \(songCacheItem.trackId)")
    }
    
    // I stopped using this as putting a key value observer on the audio player seemed better
    func playerItemPlaybackStalled(_ playerItem: CachingPlayerItem) {
//        GGLog.warning("Player item playback was stalled for track ID \(songCacheItem.trackId)")
//        playbackStalledTrackId = songCacheItem.trackId
    }
}

class TimeControlStatusObserver : NSObject {
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "timeControlStatus" {
            AudioPlayer.onTimeControlStatusChange()
        }
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
