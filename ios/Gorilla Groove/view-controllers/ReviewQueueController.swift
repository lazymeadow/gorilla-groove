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
        let layout = UICollectionViewFlowLayout()
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)

        layout.scrollDirection = .horizontal
        layout.minimumInteritemSpacing = 0
        layout.minimumLineSpacing = 0
        
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        
        collectionView.isPagingEnabled = true
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(ReviewTrackCollectionCell.self, forCellWithReuseIdentifier: "trackCell")
        collectionView.alwaysBounceVertical = true
        collectionView.alwaysBounceVertical = false
        
        // Only want one track visible at a time, so make the item size be the same as the screen width
        let screenWidth = self.view.frame.size.width
        let collectionViewHeight = screenWidth + 30
        layout.itemSize = CGSize(width: screenWidth, height: collectionViewHeight)
        
        return collectionView
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = ReviewQueueController.title
        
        self.view.addSubview(queueSelectionView)
        self.view.addSubview(collectionView)
        self.view.addSubview(activitySpinner)

        self.view.backgroundColor = Colors.background
        
        NowPlayingTracks.addTrackChangeObserver { _ in self.handleTrackChange() }
        
        NSLayoutConstraint.activate([
            collectionView.leftAnchor.constraint(equalTo: self.view.leftAnchor),
            collectionView.rightAnchor.constraint(equalTo: self.view.rightAnchor),
            collectionView.topAnchor.constraint(equalTo: self.view.topAnchor),
            collectionView.heightAnchor.constraint(equalToConstant: (collectionView.collectionViewLayout as! UICollectionViewFlowLayout).itemSize.height),
            
            queueSelectionView.leftAnchor.constraint(equalTo: self.view.leftAnchor),
            queueSelectionView.rightAnchor.constraint(equalTo: self.view.rightAnchor),
            queueSelectionView.bottomAnchor.constraint(equalTo: self.view.safeAreaLayoutGuide.bottomAnchor),
            
            activitySpinner.bottomAnchor.constraint(equalTo: collectionView.bottomAnchor, constant: -26),
            activitySpinner.centerXAnchor.constraint(equalTo: collectionView.centerXAnchor),
        ])
    }
    
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        if self.selectedSourceId == nil {
            initData()
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        GGNavLog.info("Loaded review queue")
        
        TrackSynchronizer.registerReviewQueueController(self)

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
        for sourceType in [SourceType.USER_RECOMMEND, .ARTIST, .YOUTUBE_CHANNEL] {
            if let firstSourceForType = sourceByType[sourceType]?.first {
                self.selectedSourceId = firstSourceForType.id
                break
            }
        }
        
        if let sourceId = self.selectedSourceId {
            DispatchQueue.main.async {
                self.setActiveSource(sourceId)
            }
        }
    }
    
    func setActiveSource(_ sourceId: Int) {
        self.selectedSourceId = sourceId
        let source = self.sourceIdToSource[sourceId]!
        self.visibleTracks = self.tracksForSource[sourceId] ?? []
        
        activeSourceLabel.text = source.displayName
        activeSourceLabel.sizeToFit()
        
        collectionView.reloadData()
    }
    
    
    @objc func pickSource(tapGestureRecognizer: UITapGestureRecognizer) {
        GGNavLog.info("User tapped Pick Source")
        queueSelectionView.animateBackgroundColor(color: UIColor("333333"))
        
        let vc = PickReviewSourceController(
            activeSourceId: self.selectedSourceId!,
            reviewSources: self.reviewSourcesNeedingReview,
            reviewQueueController: self
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
        playNext: Bool = false
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
        
        NowPlayingTracks.removeTracks(trackIdsToRemove)
        
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
        cell.tableIndex = indexPath.row
        
        let track = self.visibleTracks[indexPath.item]
        cell.track = track
        
        cell.startPlayView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(playActiveSong(sender:))))
        cell.rejectIcon.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(reject(sender:))))
        cell.acceptIcon.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(accept(sender:))))

        return cell
    }

    @objc func playActiveSong(sender: UITapGestureRecognizer) {
        GGNavLog.info("User tapped Play Active Song")
        let cell = sender.reviewTrackCell
        
        NowPlayingTracks.setNowPlayingTracks(visibleTracks, playFromIndex: cell.tableIndex)
        
        cell.updatePlayButton()
    }
    
    private func handleTrackChange() {
        DispatchQueue.main.async {
            self.visibleCell?.updatePlayButton()
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
    
    @objc func accept(sender: UITapGestureRecognizer) {
        GGNavLog.info("User tapped approve")
        activitySpinner.startAnimating()
        
        guard let track = visibleTrack else {
            Toast.show("Track could not be approved")
            return
        }
        
        HttpRequester.post("review-queue/track/\(track.id)/approve", EmptyResponse.self, nil) { response, statusCode, _ in
            DispatchQueue.main.async {
                self.activitySpinner.stopAnimating()
            }
            
            if !statusCode.isSuccessful() {
                Toast.show("Track could not be approved")
                return
            }
            
            Toast.show("\(track.name) was approved")
            
            // Temporarily update the Track in the DB. When we sync we'll update it again, but just keep our local state good until then.
            track.inReview = false
            track.addedToLibrary = Date()
            
            self.handleTrackChanges(updated: [track], playNext: true)
            
            TrackDao.save(track)
        }
    }
    
    @objc func reject(sender: UITapGestureRecognizer) {
        GGNavLog.info("User tapped reject")
        
        activitySpinner.startAnimating()
        
        guard let track = visibleTrack else {
            Toast.show("Track could not be rejected")
            return
        }
        
        let request = UpdateTrackRequest(trackIds: [track.id])
        HttpRequester.delete("track", request) { _, statusCode, _ in
            DispatchQueue.main.async {
                self.activitySpinner.stopAnimating()
            }
            
            if !statusCode.isSuccessful() {
                Toast.show("Track could not be rejected")
                return
            }
            
            Toast.show("\(track.name) was rejected")
                        
            self.handleTrackChanges(deleted: [track], playNext: true)
            
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
    
    var tableIndex: Int = -1

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
        playButton.tintColor = Colors.foreground
        
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



class PickReviewSourceController : UIViewController, UITableViewDataSource, UITableViewDelegate {
    let tableView = UITableView()

    let reviewSources: Array<ReviewSource>
    var activeSourceId: Int
    let reviewQueueController: ReviewQueueController
    
    init(activeSourceId: Int, reviewSources: Array<ReviewSource> = [], reviewQueueController: ReviewQueueController) {
        self.reviewSources = reviewSources
        self.activeSourceId = activeSourceId
        self.reviewQueueController = reviewQueueController
        
        super.init(nibName: nil, bundle: nil)
    }
 
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(tableView)
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        tableView.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(ReviewSourceCell.self, forCellReuseIdentifier: "reviewSourceCell")
        tableView.tableFooterView = UIView(frame: .zero)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return reviewSources.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "reviewSourceCell", for: indexPath) as! ReviewSourceCell
        let reviewSource = reviewSources[indexPath.row]
        
        cell.reviewSource = reviewSource
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(sender:)))
        cell.addGestureRecognizer(tapGesture)
            
        return cell
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        let sourceCell = cell as! ReviewSourceCell
        sourceCell.checkIfSelected(activeSourceId: activeSourceId)
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! ReviewSourceCell
        let newSourceId = cell.reviewSource!.id
        
        cell.animateSelectionColor()
        
        tableView.visibleCells.forEach { cell in
            (cell as! ReviewSourceCell).checkIfSelected(activeSourceId: newSourceId)
        }
        
        reviewQueueController.setActiveSource(newSourceId)
        
        navigationController!.popViewController(animated: true)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}



class ReviewSourceCell: UITableViewCell {
    var reviewSource: ReviewSource? {
        didSet {
            guard let reviewSource = reviewSource else { return }
            nameLabel.text = reviewSource.displayName
            nameLabel.sizeToFit()
        }
    }
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 18)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let isCheckedImage: UIView = {
        let icon = IconView("checkmark", weight: .medium, scale: .large)
        icon.translatesAutoresizingMaskIntoConstraints = false
        icon.tintColor = Colors.primary
        return icon
    }()
    
    func checkIfSelected(activeSourceId: Int) {
        isCheckedImage.isHidden = activeSourceId != reviewSource!.id
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.contentView.addSubview(nameLabel)
        self.contentView.addSubview(isCheckedImage)
        
        NSLayoutConstraint.activate([
            self.contentView.heightAnchor.constraint(equalTo: nameLabel.heightAnchor, constant: 22),
            nameLabel.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 16),
            nameLabel.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
            isCheckedImage.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor),
            isCheckedImage.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
