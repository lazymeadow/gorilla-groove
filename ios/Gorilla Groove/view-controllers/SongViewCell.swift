import UIKit
import Foundation

class SongViewCell: UITableViewCell {
    static let normalColor = UIColor.white
    static let selectionColor = UIColor(white: 0.85, alpha: 1)
    
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
        label.textColor = Colors.grey2
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let artistLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 10)
        label.textColor = Colors.grey3
        label.clipsToBounds = true
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let albumLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 14)
        label.textColor = Colors.grey3
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let durationLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 12)
        label.textColor = Colors.grey3
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    var tableIndex: Int = -1
    
    func animateSelectionColor() {
        UIView.animate(withDuration: 0.12, animations: {
            self.backgroundColor = SongViewCell.selectionColor
        }) { (finished) in
            UIView.animate(withDuration: 0.12, animations: {
                self.backgroundColor = SongViewCell.normalColor
            })
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
        self.backgroundColor = SongViewCell.normalColor
        
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
