import AVFoundation
import Foundation

class AudioPlayer {
    
    static let player = AVPlayer()
    private static var registeredCallbacks: Array<(_ time: Double)->()> = []
    
    private init() { }
    
    static func initialize() {
        player.automaticallyWaitsToMinimizeStalling = false
        player.volume = 1.0

        let time = CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player.addPeriodicTimeObserver(forInterval: time, queue: .main) { time in
            if (time.timescale.magnitude != 1_000_000_000) {
                return
            }
            
            let timeInSeconds = NSNumber(value: UInt64(time.value)).decimalValue / NSNumber(value: time.timescale.magnitude).decimalValue

            registeredCallbacks.forEach { callback in
                callback(Double(truncating: timeInSeconds as NSNumber))
            }
        }
    }
    
    static func addTimeObserver(callback: @escaping (_ time: Double) -> Void) {
        registeredCallbacks.append(callback)
    }
}
