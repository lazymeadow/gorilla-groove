import AVFoundation
import Foundation

class NowPlayingTracks {
    
    private(set) static var currentTrack: Track? = nil
    
    private(set) static var nowPlayingTracks: Array<Track> = []
    
    private static var playedShuffleIndexes: Array<Int> = []
    private static var indexesToShuffle: Array<Int> = []
    
    private static var nowPlayingIndex: Int = -1
    static var shuffleOn: Bool = false {
        didSet {
            if (shuffleOn) {
                doShuffle(preservePrevious: false)
            }
        }
    }
    
    static var repeatOn: Bool = false
    
    private static var registeredCallbacks: Array<(_ track: Track?)->()> = []
    
    private init() { }
    
    
    static func setNowPlayingTracks(_ tracks: Array<Track>, playFromIndex: Int) {
        nowPlayingTracks = tracks
        nowPlayingIndex = playFromIndex
        currentTrack = nowPlayingTracks[playFromIndex]
        
        if (shuffleOn) {
            doShuffle(preservePrevious: false)
        }

        playTrack(currentTrack!)
    }
    
    private static func playTrack(_ track: Track) {
        AudioPlayer.player.pause()
        
        HttpRequester.get("file/link/\(track.id)?audioFormat=MP3", TrackLinkResponse.self) { links, status , err in
            if (status < 200 || status >= 300 || links == nil) {
                print("Failed to get track links!")
                return
            }
            
            let playerItem = AVPlayerItem(url: URL(string: links!.songLink)!)
            
            AudioPlayer.player.replaceCurrentItem(with: playerItem)
            AudioPlayer.player.playImmediately(atRate: 1.0)
            
            notifyListeners()
        }
    }
    
    private static func notifyListeners() {
        registeredCallbacks.forEach { callback in
            callback(currentTrack)
        }
    }
    
    static func playNext() {
        if (nowPlayingTracks.isEmpty) {
            return
        }
        
        if (shuffleOn) {
            playNextShuffle()
        } else {
            playNextNonShuffle()
        }
        
        if (currentTrack == nil) {
            notifyListeners()
        } else {
            playTrack(currentTrack!)
        }
    }
    
    private static func playNextShuffle() {
        if (indexesToShuffle.isEmpty) {
            currentTrack = nil

            if (repeatOn) {
                doShuffle(preservePrevious: true)
                playNextShuffle()
            } else {
                return
            }
        } else {
            playedShuffleIndexes.append(nowPlayingIndex)

            nowPlayingIndex = indexesToShuffle.removeFirst()

            currentTrack = nowPlayingTracks[nowPlayingIndex]
        }
    }
    
    private static func playNextNonShuffle() {
        nowPlayingIndex += 1
        if (nowPlayingIndex >= nowPlayingTracks.count) {
            if (repeatOn) {
                nowPlayingIndex = 0
            } else {
                currentTrack = nil
                return
            }
        }
        
        currentTrack = nowPlayingTracks[nowPlayingIndex]
    }
    
    static func playPrevious() {
        if (nowPlayingTracks.isEmpty) {
            return
        }
        
        if (shuffleOn) {
            playPreviousShuffle()
        } else {
            playPreviousNonShuffle()
        }
        
        playTrack(currentTrack!)
    }
    
    private static func playPreviousShuffle() {
        if (playedShuffleIndexes.isEmpty) {
            // Just do nothing, which will restart the current track
        } else {
            indexesToShuffle.append(nowPlayingIndex)
            indexesToShuffle.shuffle()
            
            nowPlayingIndex = playedShuffleIndexes.removeLast()
        }

        currentTrack = nowPlayingTracks[nowPlayingIndex]
    }
    
    private static func playPreviousNonShuffle() {
        nowPlayingIndex -= 1
        if (nowPlayingIndex < 0) {
            if (repeatOn) {
                nowPlayingIndex = nowPlayingTracks.count - 1
            } else {
                nowPlayingIndex = 0 // Will restart the current track
            }
        }
        
        currentTrack = nowPlayingTracks[nowPlayingIndex]
    }
    
    private static func doShuffle(preservePrevious: Bool) {
        if (!preservePrevious) {
            playedShuffleIndexes = []
        }
        
        if (nowPlayingTracks.isEmpty) {
            return
        }

        indexesToShuffle = Array(0...nowPlayingTracks.count - 1)
        
        if (currentTrack != nil) {
            playedShuffleIndexes.append(nowPlayingIndex)
            indexesToShuffle = indexesToShuffle.filter { $0 != nowPlayingIndex }
        }
        
        indexesToShuffle.shuffle()
    }
    
    static func addTrackChangeObserver(callback: @escaping (_ track: Track?) -> Void) {
        registeredCallbacks.append(callback)
    }
}
