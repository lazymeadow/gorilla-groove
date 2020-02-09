import AVFoundation
import Foundation
import MediaPlayer

class AudioPlayer {
    
    private static let player = AVPlayer()
    private static var registeredCallbacks: Array<(_ time: Double)->()> = []
    
    private static var lastSongPlayHeartbeatTime = 0.0;
    
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

            if (CACurrentMediaTime() - lastSongPlayHeartbeatTime > 15.0) {
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
            AudioPlayer.play()
            return .success
        }
        rcc.pauseCommand.addTarget { event in
            AudioPlayer.pause()
            return .success
        }
        rcc.changePlaybackPositionCommand.addTarget { event in
            let requestedSongTime = (event as! MPChangePlaybackPositionCommandEvent).positionTime
            seekTo(requestedSongTime)

            registeredCallbacks.forEach { callback in
                callback(requestedSongTime)
            }
            return .success
        }
        rcc.nextTrackCommand.addTarget { event in
            NowPlayingTracks.playNext()
            return .success
        }
        rcc.previousTrackCommand.addTarget { event in
            NowPlayingTracks.playPrevious()
            return .success
        }
        
        rcc.playCommand.isEnabled = true
        rcc.pauseCommand.isEnabled = true
        rcc.skipBackwardCommand.isEnabled = false
        rcc.skipForwardCommand.isEnabled = false
        rcc.nextTrackCommand.isEnabled = true
        rcc.previousTrackCommand.isEnabled = true
        rcc.changePlaybackPositionCommand.isEnabled = true
        
        
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
        MPNowPlayingInfoCenter.default().nowPlayingInfo![MPNowPlayingInfoPropertyElapsedPlaybackTime] = time
    }
    
    static func addTimeObserver(callback: @escaping (_ time: Double) -> Void) {
        registeredCallbacks.append(callback)
    }
    
    static func play() {
        MPNowPlayingInfoCenter.default().nowPlayingInfo![MPNowPlayingInfoPropertyPlaybackRate] = 1.0
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
    
    static func playNewLink(_ link: String) {
        let playerItem = AVPlayerItem(url: URL(string: link)!)
        
        player.replaceCurrentItem(with: playerItem)
        player.playImmediately(atRate: 1.0)
        
        sendPlayEvent(NowPlayingTracks.currentTrack)
    }
    
    private static func sendPlayEvent(_ track: Track?) {
        let deviceId = FileState.read(DeviceState.self)!.deviceId

        let request = PlayEventRequest(trackId: track?.id, deviceId: deviceId)
        
        if (track != nil) {
            lastSongPlayHeartbeatTime = CACurrentMediaTime()
        }
        
        HttpRequester.post("currently-listening", EmptyResponse.self, request)
    }
    
    struct PlayEventRequest: Codable {
        let trackId: Int64?
        let deviceId: String
    }
}
