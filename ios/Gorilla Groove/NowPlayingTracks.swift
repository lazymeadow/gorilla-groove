import AVFoundation
import Foundation
import MediaPlayer

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
    
    private static func playTrack(_ originalTrack: Track) {
        AudioPlayer.pause()
        
        // Make sure we've got the latest information. Song may have been cached since the last view retrieved it, or whatever
        let track = TrackDao.findById(originalTrack.id)!
        
        // First check if this song is already cached so we don't have to fetch it
        if track.cachedAt != nil {
            let cachedSong = track.getCachedSongData()
            if cachedSong == nil {
                print("Failed to find cached song data, despite track thinking it had a cache! Clearing cache from DB for track \(track.id)")
                TrackDao.setCachedAt(trackId: track.id, cachedAt: nil)
            } else {
                print("Track \(track.id) is already cached. Playing from offline storage")
                AudioPlayer.playSongData(cachedSong!)
                updatePlayingTrackInfo(track)
                
                notifyListeners()
                return
            }
        }
        
        // If it is not cached, go out to the LIVE INTERNET To find it (and cache it while streaming it)
        HttpRequester.get("file/link/\(track.id)?audioFormat=MP3", TrackLinkResponse.self) { links, status , err in
            if (status < 200 || status >= 300 || links == nil) {
                print("Failed to get track links!")
                return
            }
            
            AudioPlayer.playNewLink(links!.songLink, trackId: track.id)
            setNowPlayingAlbumArt(links!.albumArtLink)
            
            updatePlayingTrackInfo(track)
            
            notifyListeners()
        }
    }
    
    private static func updatePlayingTrackInfo(_ track: Track) {
        var nowPlayingInfo = [String : Any]()
        
        nowPlayingInfo[MPMediaItemPropertyTitle] = track.name
        nowPlayingInfo[MPMediaItemPropertyArtist] = track.artist
        nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = track.album
        
        nowPlayingInfo[MPMediaItemPropertyArtwork] = nil
        
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = AudioPlayer.currentTime
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = track.length
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = AudioPlayer.rate
        
        // Set the metadata
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }
    
    private static func setNowPlayingAlbumArt(_ artLink: String?) {
        guard let link = artLink else { return }
        
        DispatchQueue.global().async {
            let url = URL(string: link)!
            
            do {
                let data = try Data.init(contentsOf: url)
                if let image = UIImage(data: data) {
                    let artwork = MPMediaItemArtwork(boundsSize: image.size, requestHandler: { _ in return image })
                
                    MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyArtwork] = artwork
                } else {
                    print("Found image data that could not be displayed. It is probably in an unsupported format like webp")
                }
            } catch {
                print(error)
            }
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
