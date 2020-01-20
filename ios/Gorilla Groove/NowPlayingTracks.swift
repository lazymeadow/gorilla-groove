import AVFoundation
import Foundation

class NowPlayingTracks {
    
    private(set) static var currentTrack: Track? = nil
    private static var registeredCallbacks: Array<(_ track: Track?)->()> = []
    
    private init() { }
    
    static func initialize() {

    }
    
    static func setCurrentTrack(_ track: Track) {
        currentTrack = track
        
        registeredCallbacks.forEach { callback in
            callback(currentTrack)
        }
    }
    
    static func addTrackChangeObserver(callback: @escaping (_ track: Track?) -> Void) {
        registeredCallbacks.append(callback)
    }
}
