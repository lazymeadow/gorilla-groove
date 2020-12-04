import AVFoundation
import Foundation
import MediaPlayer

class NowPlayingTracks {
    
    private(set) static var currentTrack: Track? = nil
    
    private(set) static var nowPlayingTracks: Array<Track> = []
    
    private static var playedShuffleIndexes: Array<Int> = []
    private static var indexesToShuffle: Array<Int> = []
    
    private(set) static var nowPlayingIndex: Int = -1
    static var shuffleOn: Bool = false {
        didSet {
            if (shuffleOn) {
                doShuffle(preservePrevious: false)
            }
            persistState()
        }
    }
    
    static var repeatOn: Bool = false {
        didSet {
            persistState()
        }
    }
    
    private static var registeredCallbacks: Array<(_ track: Track?)->()> = []
    
    private init() { }
    
    static func initialize() {
        let mediaState = FileState.read(MediaState.self) ?? MediaState(shuffleEnabled: false, repeatEnabled: false)
        shuffleOn = mediaState.shuffleEnabled
        repeatOn = mediaState.repeatEnabled
    }
    
    static func persistState() {
        let mediaState = MediaState(shuffleEnabled: shuffleOn, repeatEnabled: repeatOn)
        FileState.save(mediaState)
    }
    
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
        LocationService.requestLocationPermissionIfNeeded()
        
        // Make sure we've got the latest information. Song may have been cached since the last view retrieved it, or whatever
        let track = TrackDao.findById(originalTrack.id)!
        
        updatePlayingTrackInfo(track)
        notifyListeners()
        
        // First check if this song is already cached so we don't have to fetch it
        let (existingSongData, existingArtData) = getCachedData(track)
        if let existingSongData = existingSongData {
            GGLog.debug("Song data for track \(track.id) is already cached. Playing from offline storage")
            AudioPlayer.playSongData(existingSongData)
        }
        if let existingArtData = existingArtData {
            GGLog.debug("Art data for track \(track.id) is already cached. Displaying from offline storage")
            AudioPlayer.playSongData(existingArtData)
        }

        // If it is not cached, go out to the LIVE INTERNET To find it (and cache it while streaming it)
        playFromLiveInternetData(
            track: track,
            fetchSong: existingSongData == nil,
            fetchArt: existingArtData == nil && track.filesizeArtPng > 0,
            shouldCache: OfflineStorageService.shouldTrackBeCached(track: track)
        )
    }
    
    private static func getCachedData(_ track: Track) -> (Data?, Data?) {
        var cachedSong: Data? = nil
        var cachedArt: Data? = nil
        
        if track.songCachedAt != nil {
            cachedSong = CacheService.getCachedData(trackId: track.id, cacheType: .song)
            if cachedSong == nil {
                GGLog.error("Failed to find cached song data, despite the track thinking it had a cache! Clearing cache from DB for track \(track.id)")
                CacheService.deleteCachedData(trackId: track.id, cacheType: .song, ignoreWarning: true)
            }
        }
        
        if track.artCachedAt != nil {
            cachedArt = CacheService.getCachedData(trackId: track.id, cacheType: .art)
            if cachedArt == nil {
                GGLog.error("Failed to find cached art data, despite the track thinking it had a cache! Clearing cache from DB for track \(track.id)")
                CacheService.deleteCachedData(trackId: track.id, cacheType: .art, ignoreWarning: true)
            }
        }
        
        return (cachedSong, cachedArt)
    }
    
    private static func playFromLiveInternetData(track: Track, fetchSong: Bool, fetchArt: Bool, shouldCache: Bool) {
        // Specifying the links we want to fetch is a very slight optimization on the API side, as it does not have to generate
        // links for art if we are fetching just the song, or vice versa. This will rarely be separate. But if album art or
        // song data gets updated and our cache gets busted, then it could happen.
        let linkFetchType: String
        if !fetchSong && !fetchArt {
            return
        } else if fetchSong && !fetchArt {
            linkFetchType = "SONG"
        } else if !fetchSong && fetchArt {
            linkFetchType = "ART"
        } else {
            linkFetchType = "BOTH"
        }
        
        HttpRequester.get("file/link/\(track.id)?audioFormat=MP3&linkFetchType=\(linkFetchType)", TrackLinkResponse.self) { links, status , err in
            if status < 200 || status >= 300 || links == nil {
                GGLog.error("Failed to get track links!")
                return
            }
            
            if track.id != currentTrack?.id {
                GGLog.debug("Track links were fetched, but the track was changed to Track \(currentTrack?.id ?? -1) before the links returned. Not playing track \(track.id).")
                return
            }
            
            if fetchSong {
                if let songLink = links!.songLink {
                    AudioPlayer.playNewLink(songLink, trackId: track.id, shouldCache: shouldCache)
                } else {
                    GGLog.error("Fetched song links from the API for track with ID: \(track.id) but no link was returned!")
                }
            }
            
            if fetchArt {
                if let artLink = links!.albumArtLink {
                    setNowPlayingAlbumArt(track: track, artLink: artLink, shouldCache: shouldCache)
                } else {
                    GGLog.error("Fetched art links from the API for track with ID: \(track.id) but no link was returned!")
                }
            }
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
    
    private static func setNowPlayingAlbumArt(track: Track, artLink: String?, shouldCache: Bool) {
        guard let link = artLink else { return }
        if link.isEmpty { return }
        
        DispatchQueue.global().async {
            guard let image = UIImage.fromUrl(link) else {
                GGLog.error("Could not download image from link! \(link)")
                return
            }
            
            if shouldCache {
                if let pngData = image.pngData() {
                    CacheService.setCachedData(trackId: track.id, data: pngData, cacheType: .art)
                } else {
                    GGLog.error("Could not parse PNG data from image from link! \(link)")
                }
            }
            
            let artwork = MPMediaItemArtwork(boundsSize: image.size, requestHandler: { _ in return image })
            MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyArtwork] = artwork
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

struct MediaState: Codable {
    let shuffleEnabled: Bool
    let repeatEnabled: Bool
}
