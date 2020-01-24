import Foundation

class Formatters {
    
    static func timeFromSeconds(_ totalSeconds: Int) -> String {
        let seconds = totalSeconds % 60
        let minutes = totalSeconds / 60
        
        let zeroPaddedSeconds = seconds < 10 ? "0" + String(seconds) : String(seconds)
        
        return String(minutes) + ":" + zeroPaddedSeconds
    }
    
}
