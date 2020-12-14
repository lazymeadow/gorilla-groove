import Foundation

class NowPlayingController : TrackViewController {
    init() {
        super.init("Now Playing", scrollPlayedTrackIntoView: true, loadTracksFunc: {
            return NowPlayingTracks.nowPlayingTracks
        })
    }
    
    override func viewDidAppear(_ animated: Bool) {
        GGNavLog.info("Loaded now playing")
        
        super.viewDidAppear(animated)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
