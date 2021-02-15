import Foundation

class NowPlayingController : TrackViewController {
    init() {
        super.init(
            "Now Playing",
            scrollPlayedTrackIntoView: true,
            originalView: .NOW_PLAYING,
            alwaysReload: true,
            trackContextView: .NOW_PLAYING,
            loadTracksFunc: {
                return NowPlayingTracks.getNowPlayingTracks()
            }
        )
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        NowPlayingTracks.addTrackChangeObserver(self) { vc, track, type in
            DispatchQueue.main.async {
                if (type == .REMOVED || type == .ADDED || type == .RESET) && vc.viewIfLoaded?.window != nil {
                    GGLog.debug("Reloading now playing tracks because of a type change: \(type)")
                    vc.loadTracks()
                }
            }
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        GGNavLog.info("Loaded now playing")
        
        super.viewDidAppear(animated)
    }
    
    override func checkIfCellIsPlaying(_ cell: TrackViewCell, indexPath: IndexPath) {
        cell.setIsPlaying(indexPath.row == NowPlayingTracks.nowPlayingIndex)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
