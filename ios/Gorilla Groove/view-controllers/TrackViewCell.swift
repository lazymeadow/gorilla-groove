import UIKit
import Foundation

class TrackViewCell: UITableViewCell {

    var track: Track? {
        didSet {
            guard let track = track else {return}
            
            nameLabel.text = track.name.isEmpty ? " " : track.name
            artistLabel.text = track.artist.isEmpty ? " " : track.artist.uppercased()
            albumLabel.text = track.album.isEmpty ? " " : track.album
            
            durationLabel.text = Formatters.timeFromSeconds(Int(track.length))
            
            nameLabel.sizeToFit()
            artistLabel.sizeToFit()
            albumLabel.sizeToFit()
        }
    }

    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 16)
        label.textColor = UIColor(named: "Table Text")
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let artistLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 10)
        label.textColor = UIColor(named: "Artist Display")
        label.clipsToBounds = true
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let albumLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 14)
        label.textColor = UIColor(named: "Song Display")
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let durationLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 12)
        label.textColor = UIColor(named: "Artist Display")
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    var tableIndex: Int = -1

    func checkIfPlaying() {
        if (track != nil && track?.id == NowPlayingTracks.currentTrack?.id) {
            artistLabel.textColor = Colors.primary
            nameLabel.textColor = UIColor(named: "Playing Song Title")
            albumLabel.textColor = Colors.primary
        } else {
            artistLabel.textColor = UIColor(named: "Song Display")
            nameLabel.textColor = UIColor(named: "Table Text")
            albumLabel.textColor = UIColor(named: "Song Display")
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        let containerView = UIStackView()
        containerView.axis = .vertical
        containerView.distribution  = .fill
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        let topRow = UIStackView()
        topRow.axis = .horizontal
        topRow.distribution  = .equalSpacing
        topRow.alignment = .fill
        topRow.translatesAutoresizingMaskIntoConstraints = false
        
        topRow.addArrangedSubview(artistLabel)
        topRow.addArrangedSubview(durationLabel)
        
        containerView.addArrangedSubview(topRow)
        containerView.addArrangedSubview(nameLabel)
        containerView.addArrangedSubview(albumLabel)
        
        containerView.setCustomSpacing(6.0, after: topRow)
        containerView.setCustomSpacing(8.0, after: nameLabel)

        self.contentView.addSubview(containerView)
        
        NSLayoutConstraint.activate([
            containerView.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
            containerView.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 16),
            containerView.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -16),
            artistLabel.widthAnchor.constraint(equalTo: topRow.widthAnchor, constant: -40),
            topRow.widthAnchor.constraint(equalTo: containerView.widthAnchor),
            
            self.contentView.heightAnchor.constraint(equalTo: containerView.heightAnchor, constant: 10)
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
