import Foundation

class NowPlayingController : TrackViewController {
    init() {
        super.init(
            "Now Playing",
            scrollPlayedTrackIntoView: true,
            originalView: .NOW_PLAYING,
            alwaysReload: true,
            loadTracksFunc: {
                return NowPlayingTracks.getNowPlayingTracks()
            }
        )
    }
    
    override func viewDidAppear(_ animated: Bool) {
        GGNavLog.info("Loaded now playing")
        
        super.viewDidAppear(animated)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
