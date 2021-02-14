import Foundation
import UIKit
import AVKit
import FDWaveformView

class TrimTrackController : UIViewController, FDWaveformViewDelegate {
    
    private let track: Track
    private var timeToTrim: Double = 0
    private var totalTime: Double = 0 // The API only stores song length as an integer. We need more precision so calculate it separate and store it
    private var trackSampleRate: Double = -1
    private var trimmingFront: Bool = true
    
    private static var targetAudioFilePath = FileManager.default
        .urls(for: .cachesDirectory, in: .userDomainMask)[0]
        .appendingPathComponent("trim-track.mp3")
    
    private var player: AVPlayer = {
        // See the comment in AudioPlayer.swift for more info on why this is split out like this
        // https://stackoverflow.com/a/57829394/13175115
        AVPlayer()
    }()
    
    private var zoomLevel = 1
    
    // This is the upper bound as indicated by FDWaveformViewDelegate. I assume it's bitrate * track length?
    // When the user taps on the view, we will get another number and can compare it to this one to know what percentage
    // of the track they highlighted. I feel there's probably a better way but I'm not finding it.
    private var upperBound = 0
    
    // Same units as above. This is the part the user has selected.
    private var scrubbedAmount = 0
    
    private lazy var trackNameLabel: UILabel = {
        let field = UILabel()
        field.font = field.font!.withSize(20)
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textColor = Colors.foreground
        field.text = track.name
        
        return field
    }()
    
    private let trimValueLabel: UILabel = {
        let field = UILabel()
        field.font = field.font!.withSize(16)
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textColor = Colors.foreground
        field.text = "0.000 Seconds"
        field.isHidden = true
        
        return field
    }()
    
    private static let trimDirectionFontSize: CGFloat = 20
    private let trimAreaLabel: UILabel = {
        let field = UILabel()
        field.font = field.font!.withSize(trimDirectionFontSize)
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textColor = Colors.foreground
        field.text = "Trim:"
        
        return field
    }()
    
    private let trimFrontLabel: UILabel = {
        let field = UILabel()
        field.font = field.font!.withSize(trimDirectionFontSize)
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textColor = Colors.primary
        field.text = "Front"
        field.isUserInteractionEnabled = true

        return field
    }()
    
    private let slashLabel: UILabel = {
        let field = UILabel()
        field.font = field.font!.withSize(trimDirectionFontSize)
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textColor = Colors.foreground
        field.text = "/"
        
        return field
    }()
    
    private let trimBackLabel: UILabel = {
        let field = UILabel()
        field.font = field.font!.withSize(trimDirectionFontSize)
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textColor = Colors.foreground
        field.text = "Back"
        field.isUserInteractionEnabled = true
        
        return field
    }()
    
    private let quickZoomLabel: UILabel = {
        let field = UILabel()
        field.font = field.font!.withSize(trimDirectionFontSize)
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textColor = Colors.foreground
        field.text = "Quick Zoom:"
        
        return field
    }()
    
    private let trimActivitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.color = Colors.foreground
        spinner.hidesWhenStopped = true
        
        return spinner
    }()
    
    private let loadingActivitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.color = Colors.foreground
        spinner.hidesWhenStopped = true
        
        return spinner
    }()
    
    private let previewButton: GGButton = {
        let button = GGButton()
        button.setTitle("Preview", for: .normal)
        button.isHidden = true
        
        let image = SFIconCreator.create("speaker.3.fill")

        button.setImage(image, for: .normal)
        button.imageEdgeInsets = UIEdgeInsets(top: 0, left: -15, bottom: 0, right: 0)
        
        return button
    }()
    
    private lazy var increaseTrimWindowButton: IconView = {
        let icon = IconView("plus", weight: .medium, scale: .large)
        icon.translatesAutoresizingMaskIntoConstraints = false
        icon.tintColor = Colors.primary
        
        icon.addGestureRecognizer(UITapGestureRecognizer(
            target: self,
            action: #selector(quickZoomIn)
        ))
        return icon
    }()
    
    private lazy var decreaseTrimWindowButton: IconView = {
        let icon = IconView("minus", weight: .medium, scale: .large)
        icon.translatesAutoresizingMaskIntoConstraints = false
        icon.tintColor = Colors.primary

        icon.addGestureRecognizer(UITapGestureRecognizer(
            target: self,
            action: #selector(quickZoomOut)
        ))
        return icon
    }()
    
    private lazy var waveformView: FDWaveformView = {
        let view = FDWaveformView()
        view.translatesAutoresizingMaskIntoConstraints = false
        
        // I think I have to swap these colors if we trim from the back
        view.wavesColor = Colors.foreground
        view.progressColor = Colors.primary
        view.isHidden = true
        
        view.delegate = self
        
        return view
    }()
    
    func waveformDidEndScrubbing(_ waveformView: FDWaveformView) {
        guard let newScrub = waveformView.highlightedSamples?.upperBound else {
            GGLog.critical("Could not get new scrub value from waveform!")
            return
        }
        GGLog.debug("Waveform was scrubbed with the divider at \(newScrub) or as a percent: \(Double(newScrub) / Double(upperBound))")
        scrubbedAmount = newScrub
        
        recalculateTimeToTrim()
    }
    
    func recalculateTimeToTrim() {
        if trimmingFront {
            timeToTrim = Double(scrubbedAmount) / trackSampleRate
        } else {
            timeToTrim = (Double(waveformView.totalSamples) - Double(scrubbedAmount)) / trackSampleRate
        }
        
        trimValueLabel.text = String(format:"%.3f Seconds", timeToTrim)
        trimValueLabel.sizeToFit()
    }
    
    func waveformViewDidLoad(_ waveformView: FDWaveformView) {
        upperBound = waveformView.zoomSamples.upperBound
        GGLog.debug("Waveform was loaded with upper bound of \(upperBound)")
    }
    
    var firstRenderDone = false
    func waveformViewDidRender(_ waveformView: FDWaveformView) {
        if !firstRenderDone {
            firstRenderDone = true
            
            waveformView.isHidden = false
            previewButton.isHidden = false
            trimValueLabel.isHidden = false
            
            loadingActivitySpinner.stopAnimating()
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "Trim Track"
        
        self.view.backgroundColor = Colors.background
        
        self.view.addSubview(trackNameLabel)
        self.view.addSubview(waveformView)
        self.view.addSubview(trimValueLabel)
        self.view.addSubview(trimAreaLabel)
        self.view.addSubview(trimFrontLabel)
        self.view.addSubview(slashLabel)
        self.view.addSubview(trimBackLabel)
        self.view.addSubview(quickZoomLabel)
        self.view.addSubview(previewButton)
        self.view.addSubview(decreaseTrimWindowButton)
        self.view.addSubview(increaseTrimWindowButton)
        self.view.addSubview(trimActivitySpinner)
        self.view.addSubview(loadingActivitySpinner)
        
        player.automaticallyWaitsToMinimizeStalling = false
        player.volume = 1.0
        
        NSLayoutConstraint.activate([
            trackNameLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: 20),
            trackNameLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            trimAreaLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            trimAreaLabel.topAnchor.constraint(equalTo: trackNameLabel.bottomAnchor, constant: 40),
            trimFrontLabel.leadingAnchor.constraint(equalTo: trimAreaLabel.trailingAnchor, constant: 14),
            trimFrontLabel.topAnchor.constraint(equalTo: trimAreaLabel.topAnchor),
            slashLabel.leadingAnchor.constraint(equalTo: trimFrontLabel.trailingAnchor, constant: 14),
            slashLabel.topAnchor.constraint(equalTo: trimAreaLabel.topAnchor),
            trimBackLabel.leadingAnchor.constraint(equalTo: slashLabel.trailingAnchor, constant: 14),
            trimBackLabel.topAnchor.constraint(equalTo: trimAreaLabel.topAnchor),
            
            quickZoomLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            quickZoomLabel.topAnchor.constraint(equalTo: trimAreaLabel.bottomAnchor, constant: 30),
            
            increaseTrimWindowButton.leadingAnchor.constraint(equalTo: quickZoomLabel.trailingAnchor, constant: 10),
            increaseTrimWindowButton.centerYAnchor.constraint(equalTo: quickZoomLabel.centerYAnchor),
            
            decreaseTrimWindowButton.leadingAnchor.constraint(equalTo: increaseTrimWindowButton.trailingAnchor, constant: 10),
            decreaseTrimWindowButton.centerYAnchor.constraint(equalTo: quickZoomLabel.centerYAnchor),
            
            waveformView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15),
            waveformView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15),
            waveformView.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            waveformView.heightAnchor.constraint(equalToConstant: 90),
            
            trimValueLabel.topAnchor.constraint(equalTo: waveformView.bottomAnchor, constant: 20),
            trimValueLabel.centerXAnchor.constraint(equalTo: waveformView.centerXAnchor),
            
            previewButton.topAnchor.constraint(equalTo: trimValueLabel.bottomAnchor, constant: 20),
            previewButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            previewButton.widthAnchor.constraint(equalToConstant: 120),
            previewButton.heightAnchor.constraint(equalToConstant: 40),

            trimActivitySpinner.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -23),
            trimActivitySpinner.topAnchor.constraint(equalTo: view.topAnchor, constant: 10),
            
            loadingActivitySpinner.centerXAnchor.constraint(equalTo: waveformView.centerXAnchor),
            loadingActivitySpinner.centerYAnchor.constraint(equalTo: waveformView.centerYAnchor),
        ])
        
        // It's pretty much guaranteed the user will want to listen to the audio before they do the trim.
        // So get the process of downloading the track going right away.
        downloadTrackAudio()
        
        GGNavLog.info("Loaded trim track view controller with track ID: \(track.id)")
        
        previewButton.addTarget(self, action: #selector(previewTrack), for: .touchUpInside)
        trimFrontLabel.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(setTrimFront)))
        trimBackLabel.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(setTrimBack)))

        loadingActivitySpinner.startAnimating()
    }
    
    var currentPreviewId = 0
    @objc private func previewTrack() {
        AudioPlayer.pause()
        
        let playerItem = AVPlayerItem(url: TrimTrackController.targetAudioFilePath)
        player.replaceCurrentItem(with: playerItem)
        
        let previewLength = 3.0
        
        if trimmingFront {
            player.seek(to: CMTime(seconds: timeToTrim, preferredTimescale: 1000))
        } else {
            player.seek(to: CMTime(seconds: totalTime - timeToTrim - previewLength, preferredTimescale: 1000))
        }
        player.play()

        // Someone could start a new preview before the last one finished. Only pause the player if it's been 3 seconds since our last preview.
        let playbackPreviewId = Int.random(in: 0...Int(INT_MAX))
        currentPreviewId = playbackPreviewId
        
        DispatchQueue.main.asyncAfter(deadline: .now() + previewLength) { [weak self] in
            guard let this = self else { return }
            if playbackPreviewId == this.currentPreviewId {
                this.player.pause()
            }
        }
    }
    
    @objc private func quickZoomOut() {
        zoomLevel /= 2
        if zoomLevel < 1 {
            zoomLevel = 1
        }
        recalculateZoom()
    }
    @objc private func quickZoomIn() {
        zoomLevel *= 2
        recalculateZoom()
    }
    
    private func recalculateZoom() {
        if trimmingFront {
            waveformView.zoomSamples = 0 ..< (waveformView.totalSamples / zoomLevel)
        } else {
            waveformView.zoomSamples = waveformView.totalSamples - (waveformView.totalSamples / zoomLevel) ..< waveformView.totalSamples
        }
    }
    
    @objc private func setTrimFront() {
        if trimmingFront {
            return
        }
        
        trimmingFront = true
        trimFrontLabel.textColor = Colors.primary
        trimBackLabel.textColor = Colors.foreground
        
        let oldColor = waveformView.progressColor
        waveformView.progressColor = waveformView.wavesColor
        waveformView.wavesColor = oldColor
        
        // If nothing was selected, keep it that way
        if waveformView.highlightedSamples == nil || (waveformView.highlightedSamples!.lowerBound == 0 && waveformView.highlightedSamples!.upperBound == upperBound) {
            waveformView.highlightedSamples = 0..<0
            scrubbedAmount = 0
        }
        
        recalculateZoom()
        recalculateTimeToTrim()
    }
    @objc private func setTrimBack() {
        if !trimmingFront {
            return
        }
        
        trimmingFront = false
        trimFrontLabel.textColor = Colors.foreground
        trimBackLabel.textColor = Colors.primary
        
        // I don't see a way to just reverse the direction of the highlight. So to trim from behind, we have to get creative and swap
        // the colors and move the scrub amount.
        let oldColor = waveformView.progressColor
        waveformView.progressColor = waveformView.wavesColor
        waveformView.wavesColor = oldColor
        
        // If nothing was selected, keep it that way
        if waveformView.highlightedSamples?.upperBound == waveformView.highlightedSamples?.lowerBound {
            waveformView.highlightedSamples = 0..<upperBound
            scrubbedAmount = upperBound
        }
        
        recalculateZoom()
        recalculateTimeToTrim()
    }
    
    private func downloadTrackAudio() {
        // If the track is already downloaded, just copy it to a temporary location for us to mess with. Then it can't
        // be deleted out from underneath us from the cache purger or anything like that.
        if track.songCachedAt != nil {
            if let existingSongData = CacheService.getCachedData(trackId: track.id, cacheType: .song) {
                if FileManager.exists(TrimTrackController.targetAudioFilePath) {
                    GGLog.info("Trim track already existed. Deleting it")
                    try! FileManager.default.removeItem(at: TrimTrackController.targetAudioFilePath)
                }
                try! existingSongData.write(to: TrimTrackController.targetAudioFilePath)
                
                loadDownloadedTrackDataIntoView()

                GGLog.info("Preview track was copied from disk")
                return
            } else {
                GGLog.error("Existing song data should have existed, but did not. Deleting cache for the track.")
                CacheService.deleteCachedData(trackId: track.id, cacheType: .song)
            }
        }
        
        TrackService.fetchLinksForTrack(track: track, fetchSong: true, fetchArt: false) { [weak self] trackLinks in
            guard let this = self else { return }
            
            guard let songLink = trackLinks?.songLink else {
                DispatchQueue.main.async {
                    this.loadingActivitySpinner.stopAnimating()
                    Toast.show("Could not load track audio", view: this.view)
                }
                GGLog.error("Could not get a song link from API for track \(this.track.id)")
                
                return
            }
            
            HttpRequester.download(songLink) { fileOnDiskUrl in
                guard let fileOnDiskUrl = fileOnDiskUrl else {
                    GGLog.error("Could not download track audio for track with ID: \(this.track.id)")
                    DispatchQueue.main.async {
                        this.loadingActivitySpinner.stopAnimating()
                        Toast.show("Could not load track audio", view: this.view)
                    }
                    
                    return
                }
                
                if FileManager.exists(TrimTrackController.targetAudioFilePath) {
                    GGLog.info("Trim track already existed. Deleting it")
                    try! FileManager.default.removeItem(at: TrimTrackController.targetAudioFilePath)
                }
                try! FileManager.default.moveItem(at: fileOnDiskUrl, to: TrimTrackController.targetAudioFilePath)
                
                this.loadDownloadedTrackDataIntoView()
                
                GGLog.info("Preview track finished downloading")
            }
        }
    }
    
    private func loadDownloadedTrackDataIntoView() {
        waveformView.audioURL = TrimTrackController.targetAudioFilePath
        
        // The waveform tells us how much is selected based off the sample rate. So we need to know the sample rate of the audio file
        // in order to get the length of time to trim.
        trackSampleRate = 5
        
        let asset = AVAsset(url: TrimTrackController.targetAudioFilePath)
        if let avAsset = asset.tracks.first {
            let desc = avAsset.formatDescriptions[0] as! CMAudioFormatDescription
            let basic = CMAudioFormatDescriptionGetStreamBasicDescription(desc)
            trackSampleRate = basic?.pointee.mSampleRate ?? -1
            
            let duration = asset.duration
            totalTime = CMTimeGetSeconds(duration)
            
            GGLog.debug("Track sample rate is \(trackSampleRate) and total length of time is \(totalTime)")
        } else {
            GGLog.error("Could not parse sample rate from track!")
            Toast.show("Could not load track data to trim", view: view)
        }
    }
    
    init(_ track: Track) {
        self.track = track
        
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
