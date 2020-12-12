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
    
    // This is used to determine cache eviction policy.
    private static func updateStartedOnDevice(_ track: Track) {
        // The reason for marking this on start, and not at the same 60% listened that we normally do,
        // is just that it greatly simplifies the cache eviction process. Because song caching is async, the cache clearing code
        // that runs afterwards needs to be async. So it isn't tied to the 60%, and they can't really be tied together.
        // So it's easier to just mark the song as listened to right away so it's always done by the time the cache clearing runs.
        // A delay is put in here just to not mark something as listened to if someone skips through multiple songs quickly
        DispatchQueue.global().asyncAfter(deadline: .now() + 10.0) {
            // If the current track isn't this track, then the song was skipped in the 10 seconds. Don't mark it.
            // The exception here is if the song is incredibly short. If it is, it's not really worth not making it.
            if track.id == currentTrack?.id || track.length < 12 {
                TrackDao.setDevicePlayStart(trackId: track.id, date: Date())
            } else {
                GGLog.debug("Not updated started on time for track \(track.id) as it is no longer being played")
            }
        }
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
            updateStartedOnDevice(track)
        }
        if let existingArtData = existingArtData {
            GGLog.debug("Art data for track \(track.id) is already cached. Displaying from offline storage")
            displayImageData(trackId: track.id, data: existingArtData)
        }

        // If it is not cached, go out to the LIVE INTERNET To find it (and cache it while streaming it)
        playFromLiveInternetData(
            track: track,
            fetchSong: existingSongData == nil,
            fetchArt: existingArtData == nil && track.filesizeArtPng > 0,
            shouldCache: OfflineStorageService.shouldTrackBeCached(track: track)
        )
    }
    
    private static func displayImageData(trackId: Int, data: Data) {
        if let image = UIImage.init(data: data) {
            let artwork = MPMediaItemArtwork(boundsSize: image.size, requestHandler: { _ in return image })
            MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyArtwork] = artwork
        } else {
            GGLog.error("Could not load cached image art data! Deleting this item from cache instead")
            CacheService.deleteCachedData(trackId: trackId, cacheType: .art)
        }
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
        TrackService.fetchLinksForTrack(track: track, fetchSong: fetchSong, fetchArt: fetchArt) { trackLinks in
            guard let trackLinks = trackLinks else { return }
            
            if track.id != currentTrack?.id {
                GGLog.debug("Track links were fetched, but the track was changed to Track \(currentTrack?.id ?? -1) before the links returned. Not playing track \(track.id).")
                return
            }
            
            if fetchSong {
                // If no song link was fetched despite us asking for one, then something went really wrong and we shouldn't continue.
                guard let songLink = trackLinks.songLink else { return }
                AudioPlayer.playNewLink(songLink, trackId: track.id, shouldCache: shouldCache)
                updateStartedOnDevice(track)
            }
            
            if fetchArt {
                setNowPlayingAlbumArt(track: track, artLink: trackLinks.albumArtLink, shouldCache: shouldCache)
            }
            
            if shouldCache {
                OfflineStorageService.enqueueDelayedStorageRecalculation(delaySeconds: 30.0)
            }
        }
    }
    
    private static func updatePlayingTrackInfo(_ track: Track) {
        var nowPlayingInfo = [String : Any]()
        
        nowPlayingInfo[MPMediaItemPropertyTitle] = track.name
        nowPlayingInfo[MPMediaItemPropertyArtist] = track.artistString
        nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = track.album
        
        nowPlayingInfo[MPMediaItemPropertyArtwork] = nil
        
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = 0
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = track.length
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = 0
        
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
            
            guard let pngData = image.pngData() else {
                GGLog.error("Could not parse PNG data from link! \(link)")
                return
            }
            
            if shouldCache {
                CacheService.setCachedData(trackId: track.id, data: pngData, cacheType: .art)
            }
            
            displayImageData(trackId: track.id, data: pngData)

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
