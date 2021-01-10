import Foundation
import UIKit

class ReviewQueueController : UIViewController {

    static let title = "Review Queue"
    
    private var allReviewSources: [ReviewSource] = []
    private var sourceIdToSource: [Int: ReviewSource] = [:]
    private var reviewSourcesNeedingReview: [ReviewSource] = []
    private var tracksForSource: [Int: [Track]] = [:]
    private var visibleTracks: [Track] = []
    private var selectedSourceId: Int? = nil
    private var activeTrack: Track? = nil
    
    private var activeTrackSongLink: String? = nil
    
    private var fullReloadRequired: Bool = false
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = true
        spinner.color = Colors.foreground
        
        return spinner
    }()

    private let activeSourceLabel: UILabel = {
        let label = UILabel()
        
        label.textColor = Colors.foreground
        label.font = label.font.withSize(20)
        label.textAlignment = .left
        label.translatesAutoresizingMaskIntoConstraints = false
        
        return label
    }()
    
    lazy var queueSelectionView: UIView = {
        let rightChevron = IconView("chevron.right", weight: .medium)
        rightChevron.translatesAutoresizingMaskIntoConstraints = false
        rightChevron.tintColor = Colors.foreground
        
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = Colors.navigationBackground
                
        view.addSubview(activeSourceLabel)
        view.addSubview(rightChevron)
        
        NSLayoutConstraint.activate([
            view.heightAnchor.constraint(equalTo: activeSourceLabel.heightAnchor, constant: 20),
            activeSourceLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            activeSourceLabel.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 15),
            rightChevron.rightAnchor.constraint(equalTo: view.rightAnchor),
            rightChevron.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
        
        view.addGestureRecognizer(UITapGestureRecognizer(
            target: self,
            action: #selector(pickSource(tapGestureRecognizer:))
        ))
        
        return view
    }()
    
    private lazy var collectionView: UICollectionView = {
        let layout = ReviewQueueFlowLayout()
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)

        layout.scrollDirection = .horizontal
        layout.minimumInteritemSpacing = 0
        layout.minimumLineSpacing = 0
        
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        
        collectionView.isPagingEnabled = true
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(ReviewTrackCollectionCell.self, forCellWithReuseIdentifier: "trackCell")
        collectionView.alwaysBounceVertical = false
        
        return collectionView
    }()
    
    private let noTracksView: UIView = {
        let view = UIView()
        
        view.backgroundColor = Colors.background
        view.translatesAutoresizingMaskIntoConstraints = false
        
        let label = UILabel()
        label.text = "No more tracks to review"
        label.font = label.font.withSize(20)
        label.textColor = Colors.foreground
        label.textAlignment = .center
        label.sizeToFit()
        label.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(label)
        
        NSLayoutConstraint.activate([
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -100),
            label.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 10),
            label.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -10),
        ])
        
        return view
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = ReviewQueueController.title
        
        self.navigationItem.rightBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .edit,
            target: self,
            action: #selector(editReviewSources)
        )
        
        // viewDidAppear is not called when the app is unlocked, but we need to update the UI to the current song
        // if the song changed while the phone was locked. So add an observer for it.
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(willEnterForeground),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )

        let selectionBottomBorder = createBorder()

        self.view.addSubview(queueSelectionView)
        self.view.addSubview(selectionBottomBorder)
        self.view.addSubview(collectionView)
        self.view.addSubview(activitySpinner)
        
        self.view.addSubview(noTracksView)

        noTracksView.isHidden = true
        
        self.view.backgroundColor = Colors.background
        
        NowPlayingTracks.addTrackChangeObserver { newTrack in self.handleTrackChange(newTrack) }
        
        NSLayoutConstraint.activate([
            queueSelectionView.leftAnchor.constraint(equalTo: self.view.leftAnchor),
            queueSelectionView.rightAnchor.constraint(equalTo: self.view.rightAnchor),
            queueSelectionView.topAnchor.constraint(equalTo: self.view.safeAreaLayoutGuide.topAnchor),
            
            selectionBottomBorder.topAnchor.constraint(equalTo: queueSelectionView.bottomAnchor),
            selectionBottomBorder.heightAnchor.constraint(equalToConstant: 0.3), // Idk why, but 0.3 makes it look right. Much lower and it's invisible, at 0.5 it's darker than the navigation bar shadow
            selectionBottomBorder.leftAnchor.constraint(equalTo: queueSelectionView.leftAnchor),
            selectionBottomBorder.rightAnchor.constraint(equalTo: queueSelectionView.rightAnchor),

            collectionView.leftAnchor.constraint(equalTo: self.view.leftAnchor),
            collectionView.rightAnchor.constraint(equalTo: self.view.rightAnchor),
            collectionView.topAnchor.constraint(equalTo: selectionBottomBorder.bottomAnchor, constant: 15),
            collectionView.bottomAnchor.constraint(equalTo: self.view.safeAreaLayoutGuide.bottomAnchor),
            
            activitySpinner.bottomAnchor.constraint(equalTo: collectionView.bottomAnchor, constant: -26),
            activitySpinner.centerXAnchor.constraint(equalTo: collectionView.centerXAnchor),
            
            noTracksView.widthAnchor.constraint(equalTo: self.view.widthAnchor),
            noTracksView.heightAnchor.constraint(equalTo: self.view.heightAnchor),
        ])
    }
    
    private func createBorder() -> UIView {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = UINavigationBarAppearance().shadowColor
        
        return view
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        if self.selectedSourceId == nil {
            initData()
        } else if let currentTrack = NowPlayingTracks.currentTrack, currentTrack.inReview, currentTrack.reviewSourceId == self.selectedSourceId {
            if let index = self.visibleTracks.index(where: { $0.id == currentTrack.id }) {
                self.collectionView.scrollToItem(at: IndexPath(item: index, section: 0), at: .centeredHorizontally, animated: false)
            } else {
                GGLog.critical("Could not find index to scroll to despite current played track being in review for this source!")
            }
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        GGNavLog.info("Loaded review queue")
        
        TrackSynchronizer.registerReviewQueueController(self)
        AudioPlayer.registerReviewQueueController(self)
        
        // During big changes thinga can get out of sync (like adding review sources). This is truly just me being way too
        // lazy to want to keep things in sync myself. It doesn't NEED to be handled like this. But syncing review sources
        // is such a rare thing that I'm ok with being heavy-handed in my approach here for the sake of my own sanity.
        // The downside is that any state the user had on this controller will be reset to the default. Oh no....
        if fullReloadRequired {
            fullReloadRequired = false
            initData()
        } else {
            // Make sure the already visible cells are up to date if someone changes the screen and then comes back in
            self.collectionView.visibleCells.forEach { cell in
                (cell as! ReviewTrackCollectionCell).updatePlayButton()
            }
        }
    }
    
    @objc func willEnterForeground() {
        if self.viewIfLoaded?.window != nil {
            viewWillAppear(false)
            viewDidAppear(false)
        }
    }
    
    @objc func editReviewSources() {
        GGNavLog.info("User tapped edit review sources")
        let vc = EditReviewSourcesController(
            activeSourceId: self.selectedSourceId,
            reviewSources: self.allReviewSources,
            reviewQueueController: self
        )
        vc.modalPresentationStyle = .fullScreen
        
        self.navigationController!.pushViewController(vc, animated: true)
    }
    
    private func initData() {
        DispatchQueue.global().async {
            self.allReviewSources = ReviewSourceDao.getSources()
            var newSourceIdToSource: [Int: ReviewSource] = [:]
            self.allReviewSources.forEach { source in
                newSourceIdToSource[source.id] = source
            }
            
            self.sourceIdToSource = newSourceIdToSource
            
            var newTracksForSource: [Int: [Track]] = [:]
            var newSourcesNeedingReview: [ReviewSource] = []
            
            for track in TrackDao.getUnreviewedTracks() {
                guard let sourceId = track.reviewSourceId else {
                    GGLog.critical("Track \(track.id) was in review, but had no reviewSourceId!")
                    continue
                }
                if newTracksForSource[sourceId] == nil {
                    newTracksForSource[sourceId] = Array()
                    guard let reviewSource = newSourceIdToSource[sourceId] else {
                        GGLog.error("No review source was found for a Track ID \(track.id) that has review source ID: \(sourceId)!")
                        continue
                    }
                    newSourcesNeedingReview.append(reviewSource)
                }
                newTracksForSource[sourceId]!.append(track)
            }
            
            self.tracksForSource = newTracksForSource
            self.reviewSourcesNeedingReview = newSourcesNeedingReview
            
            self.setDefaultActiveSource()
        }
    }
    
    private func setDefaultActiveSource() {
        // Iterate over all the sources that need review, and pick a default.
        // Order goes User Recommend -> Artist -> YT Channel to put the more interesting sources first
        let sourceByType = Dictionary(grouping: self.reviewSourcesNeedingReview, by: { $0.sourceType })
        var newSourceId: Int? = nil
        for sourceType in [SourceType.USER_RECOMMEND, .ARTIST, .YOUTUBE_CHANNEL] {
            if let firstSourceForType = sourceByType[sourceType]?.first {
                newSourceId = firstSourceForType.id
                break
            }
        }
        
        DispatchQueue.main.async {
            self.setActiveSource(newSourceId)
        }
    }
    
    func setActiveSource(_ sourceId: Int?) {
        guard let sourceId = sourceId else {
            noTracksView.isHidden = false
            self.selectedSourceId = nil
            return
        }
        
        noTracksView.isHidden = true
        
        if self.selectedSourceId == sourceId {
            return
        }
        
        self.selectedSourceId = sourceId
        let source = self.sourceIdToSource[sourceId]!
        self.visibleTracks = self.tracksForSource[sourceId] ?? []
        
        activeSourceLabel.text = source.displayName
        activeSourceLabel.sizeToFit()
        
        collectionView.reloadData()
        
        self.collectionView.scrollToItem(at: IndexPath(item: 0, section: 0), at: .centeredHorizontally, animated: false)
    }
    
    @objc func pickSource(tapGestureRecognizer: UITapGestureRecognizer) {
        GGNavLog.info("User tapped Pick Source")
        queueSelectionView.animateBackgroundColor(color: UIColor("333333"))
        
        let vc = PickReviewSourceController(
            activeSourceId: self.selectedSourceId!,
            reviewSources: self.reviewSourcesNeedingReview,
            reviewQueueController: self,
            tracksForSource: tracksForSource
        )
        vc.modalPresentationStyle = .fullScreen
        
        self.navigationController!.pushViewController(vc, animated: true)
    }
    
    func onSourcesSynced() {
        fullReloadRequired = true
    }
    
    func handleTrackChanges(
        new: Array<Track> = [],
        updated: Array<Track> = [],
        deleted: Array<Track> = [],
        playNext: Bool? = nil
    ) {
        GGLog.info("Handling track changes to current controller state. Currently loaded review source is \(self.selectedSourceId ?? -1)")
        
        for track in new {
            GGLog.info("Adding new track with ID \(track.id) to review source \(track.reviewSourceId ?? -1)")
            if tracksForSource[track.reviewSourceId!] == nil {
                
                if let sourceToAdd = sourceIdToSource[track.reviewSourceId!] {
                    tracksForSource[track.reviewSourceId!] = []
                    reviewSourcesNeedingReview.append(sourceToAdd)
                } else {
                    GGLog.warning("A track was added for a review source not currently known about by the view.")
                    // We can't simply reload all review sources right now as there is no guarantee the source has been synced anyway.
                    // If someone added a review source and a track on that source at the same time, the track sync might have finished
                    // first as they're both async. So just queue up an aggressive reload later as this is a rare situation.
                    fullReloadRequired = true
                }
            }
            
            tracksForSource[track.reviewSourceId!]!.append(track)
            if selectedSourceId == track.reviewSourceId! {
                visibleTracks.append(track)
            }
        }
        
        // I don't currently care enough to deal with tracks that were updated, but are still in review.
        // This (currently) only means that someone on web skipped reviewing a song, so the downside is that we won't
        // have the same order as web has until we fully refresh all the queues. Not a big deal.
        
        let approvedTrackIds = Set(updated.filter { !$0.inReview }.map { $0.id })
        let deletedTrackIds = Set(deleted.map { $0.id })
        
        let trackIdsToRemove = deletedTrackIds.union(approvedTrackIds)
        
        if !trackIdsToRemove.isEmpty {
            GGLog.info("Removing tracks from controller state that were in review with IDs \(trackIdsToRemove)")
            
            tracksForSource.keys.forEach { sourceId in
                if tracksForSource[sourceId] != nil {
                    tracksForSource[sourceId]?.removeAll(where: { trackIdsToRemove.contains($0.id) })
                }
            }
            
            if let selectedSourceId = self.selectedSourceId {
                self.visibleTracks = tracksForSource[selectedSourceId] ?? []
            }
        }
        
        // Make sure the sources needing review is up to date. New things were added and / or removed and this could have changed.
        reviewSourcesNeedingReview = allReviewSources.filter { source in
            return !(tracksForSource[source.id] ?? []).isEmpty
        }
        
        NowPlayingTracks.removeTracks(trackIdsToRemove, playNext: playNext)
        
        if self.selectedSourceId == nil || visibleTracks.isEmpty {
            self.setDefaultActiveSource()
        } else {
            // Only need to do this if we didn't set the default source as it reloads data on its own
            DispatchQueue.main.async {
                self.collectionView.reloadData()
            }
        }
    }
}

class ReviewQueueFlowLayout: UICollectionViewFlowLayout {
    override func prepare() {
        super.prepare()
        
        guard let collectionView = self.collectionView else { return }
        
        // Only want one track visible at a time, so make the item size be the same as the screen width
        self.itemSize = CGSize(width: collectionView.frame.width, height: collectionView.frame.height)
    }
}

extension ReviewQueueController: UICollectionViewDelegate {
    // This is called when a cell actually becomes visible. So any visual state updating needs to happen here
    func collectionView(_ collectionView: UICollectionView, willDisplay cell: UICollectionViewCell, forItemAt indexPath: IndexPath) {
        (cell as! ReviewTrackCollectionCell).updatePlayButton()
    }
}

extension ReviewQueueController: UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return self.visibleTracks.count
    }
    
    // This is called when a cell is dequeued, which happens BEFORE a cell is visible. This happens because UITableCollectionView
    // has a property called "isPrefetchingEnabled", which seems good. So this cell will preemptively request album art.
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "trackCell", for: indexPath) as! ReviewTrackCollectionCell
        
        let track = self.visibleTracks[indexPath.item]
        cell.track = track
        
        cell.startPlayView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(playActiveSong(sender:))))
        cell.rejectIcon.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(rejectInternally(sender:))))
        cell.acceptIcon.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(acceptInternally(sender:))))

        return cell
    }

    @objc func playActiveSong(sender: UITapGestureRecognizer) {
        GGNavLog.info("User tapped Play Active Song")
        let cell = sender.reviewTrackCell
        
        let trackIndex = visibleTracks.index { $0.id == cell.track!.id }!
        
        NowPlayingTracks.setNowPlayingTracks(visibleTracks, playFromIndex: trackIndex)
        
        cell.updatePlayButton()
    }
    
    private func handleTrackChange(_ newTrack: Track?) {
        // There won't be a visible cell if someone has been on the lock screen
        guard let visibleCell = self.visibleCell else { return }
        
        let nextTrackIndex = visibleTracks.index { $0.id == newTrack?.id }
        
        DispatchQueue.main.async {
            if let index = nextTrackIndex {
                self.collectionView.scrollToItem(at: IndexPath(item: index, section: 0), at: .centeredHorizontally, animated: true)
            } else {
                visibleCell.updatePlayButton()
            }
        }
    }
    
    var visibleCell: ReviewTrackCollectionCell? {
        get {
            return self.collectionView.visibleCells.first as? ReviewTrackCollectionCell
        }
    }
    
    var visibleTrack: Track? {
        get {
            guard let visibleCell = visibleCell else {
                GGLog.error("Could not get visible cell from collection view!")
                return nil
            }
            
            return visibleCell.track
        }
    }
    
    func acceptFromLockScreen() {
        guard let track = NowPlayingTracks.currentTrack else {
            GGLog.critical("A track was accepted from the lock screen but it could not be found")
            return
        }
        
        accept(track)
    }
    
    @objc private func acceptInternally(sender: UITapGestureRecognizer) {
        guard let track = visibleTrack else {
            Toast.show("Track could not be approved")
            GGLog.critical("A track was accepted internally but could not be found")
            return
        }
        
        accept(track)
    }
    
    private func accept(_ track: Track) {
        GGNavLog.info("User tapped approve")
        activitySpinner.startAnimating()
        
        HttpRequester.post("review-queue/track/\(track.id)/approve", EmptyResponse.self, nil) { response, statusCode, _ in
            if !statusCode.isSuccessful() {
                DispatchQueue.main.async {
                    self.activitySpinner.stopAnimating()
                    Toast.show("Track could not be approved")
                }
                return
            }
            
            // Temporarily update the Track in the DB. When we sync we'll update it again, but just keep our local state good until then.
            track.inReview = false
            track.addedToLibrary = Date()
            
            DispatchQueue.main.async {
                Toast.show("\(track.name) was approved")
                self.activitySpinner.stopAnimating()
                self.handleTrackChanges(updated: [track])
            }
            
            TrackDao.save(track)
        }
    }
    
    func rejectFromLockScreen() {
        guard let track = NowPlayingTracks.currentTrack else {
            GGLog.critical("A track was accepted from the lock screen but it could not be found")
            return
        }
        
        reject(track)
    }
    
    @objc private func rejectInternally(sender: UITapGestureRecognizer) {
        guard let track = visibleTrack else {
            Toast.show("Track could not be rejected")
            GGLog.critical("A track was rejected internally but could not be found")
            return
        }
        
        reject(track)
    }
    
    private func reject(_ track: Track) {
        GGNavLog.info("User tapped reject")
        
        activitySpinner.startAnimating()
        
        let playNext = !AudioPlayer.isPaused
        
        // If someone rejects a song, they don't like it, and we probably should stop subjecting them to the song
        if track.id == NowPlayingTracks.currentTrack?.id {
            AudioPlayer.pause()
        }
        
        let request = UpdateTrackRequest(trackIds: [track.id])
        HttpRequester.delete("track", request) { _, statusCode, _ in
            if !statusCode.isSuccessful() {
                DispatchQueue.main.async {
                    self.activitySpinner.stopAnimating()
                    Toast.show("Track could not be rejected")
                }
                return
            }
            
            DispatchQueue.main.async {
                Toast.show("\(track.name) was rejected")
                self.activitySpinner.stopAnimating()
                self.handleTrackChanges(deleted: [track], playNext: playNext)
            }
            
            TrackDao.delete(track.id)
        }
    }
}

fileprivate extension UITapGestureRecognizer {
    // Hella dumb, but the fact that you can't pass parameters to tap gesture recognizers is even more dumb. Thanks Apple.
    // So this is where we are.
    var reviewTrackCell: ReviewTrackCollectionCell {
        var currentView = self.view
        while !(currentView is ReviewTrackCollectionCell) {
            currentView = currentView!.superview
        }
        
        return currentView as! ReviewTrackCollectionCell
    }
}

class ReviewTrackCollectionCell : UICollectionViewCell {
    
    var track: Track? {
        didSet {
            albumArtView.image = nil

            guard let track = track else {
                songTextLabel.text = ""
                return
            }
            
            if track.name.isEmpty || track.artist.isEmpty {
                songTextLabel.text = track.name + track.artist
            } else {
                songTextLabel.text = track.name + " - " + track.artist
            }
            
            updatePlayButton()
            
            if track.hasAlbumArt {
                TrackService.fetchLinksForTrack(
                    track: track,
                    fetchSong: false,
                    fetchArt: true
                ) { trackLinkResponse in
                    if self.track?.id == track.id {
                        guard let response = trackLinkResponse else {
                            Toast.show("Could not fetch album art")
                            self.setNoAlbumArtPicture()

                            return
                        }
                        
                        if let artUrl = response.albumArtLink, !artUrl.isEmpty {
                            guard let image = UIImage.fromUrl(artUrl) else {
                                GGLog.error("Could not display album art from URL \(artUrl)")
                                Toast.show("Could not display album art")
                                self.setNoAlbumArtPicture()

                                return
                            }
                            
                            DispatchQueue.main.async {
                                self.albumArtView.contentMode = .scaleAspectFit
                                self.albumArtView.image = image
                            }
                        }
                    }
                }
            } else {
                self.setNoAlbumArtPicture()
            }
        }
    }
    
    private func setNoAlbumArtPicture() {
        albumArtView.contentMode = .center
        albumArtView.image = SFIconCreator.create("music.note", weight: .light, scale: .large, multiplier: 6.0)
    }
    
    private let albumArtView: UIImageView = {
        let view = UIImageView()
        
        view.translatesAutoresizingMaskIntoConstraints = false
        view.tintColor = Colors.foreground

        return view
    }()
    
    private let songTextLabel: UILabel = {
        let label = UILabel()
        
        label.textColor = Colors.foreground
        label.font = label.font.withSize(17)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        
        return label
    }()
    
    let startPlayView: UIView = {
        let playButton = IconView("play.fill", weight: .light, scale: .large, multiplier: 6.0)
        playButton.translatesAutoresizingMaskIntoConstraints = false
        playButton.tintColor = Colors.white
        
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.backgroundColor = UIColor("1C1C1E", 0.93)
        
        view.addSubview(playButton)
        
        NSLayoutConstraint.activate([
            playButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            playButton.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
        
        return view
    }()
    
    let rejectIcon: UIView = {
        let icon = IconView("hand.thumbsdown.fill", weight: .medium, scale: .large)
        
        icon.tintColor = UIColor("CC6666")

        return icon
    }()

    let acceptIcon: UIView = {
        let icon = IconView("hand.thumbsup.fill", weight: .medium, scale: .large)
        
        icon.tintColor = UIColor("5BB47E")

        return icon
    }()
    
    func updatePlayButton() {
        startPlayView.isHidden = NowPlayingTracks.currentTrack?.id == self.track?.id
    }
    
    override init(frame: CGRect) {
        super.init(frame: .zero)
        
        let actionRow = createActionsRow()
        
        self.backgroundColor = Colors.background
        
        self.contentView.addSubview(albumArtView)
        self.contentView.addSubview(songTextLabel)
        self.contentView.addSubview(startPlayView)
        self.contentView.addSubview(actionRow)

        NSLayoutConstraint.activate([
            albumArtView.centerXAnchor.constraint(equalTo: self.contentView.centerXAnchor),
            
            albumArtView.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 35),
            albumArtView.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -35),
            albumArtView.heightAnchor.constraint(equalTo: albumArtView.widthAnchor),

            startPlayView.leadingAnchor.constraint(equalTo: albumArtView.leadingAnchor),
            startPlayView.trailingAnchor.constraint(equalTo: albumArtView.trailingAnchor),
            startPlayView.topAnchor.constraint(equalTo: albumArtView.topAnchor),
            startPlayView.bottomAnchor.constraint(equalTo: albumArtView.bottomAnchor),

            songTextLabel.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 10),
            songTextLabel.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -10),
            songTextLabel.topAnchor.constraint(equalTo: albumArtView.bottomAnchor, constant: 10),
            
            actionRow.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 55),
            actionRow.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -55),
            actionRow.topAnchor.constraint(equalTo: self.songTextLabel.bottomAnchor, constant: 10),
        ])
    }
    
    private func createActionsRow() -> UIView {
        let elements = UIStackView()
        elements.translatesAutoresizingMaskIntoConstraints = false
        elements.axis = .horizontal
        elements.distribution  = .equalSpacing
        
        elements.addArrangedSubview(rejectIcon)
        elements.addArrangedSubview(acceptIcon)
        
        return elements
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
