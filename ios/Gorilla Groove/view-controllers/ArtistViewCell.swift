import UIKit
import Foundation

class ArtistViewCell: UITableViewCell {
    
    var artist: String? {
        didSet {
            guard let artist = artist else { return }
            nameLabel.text = artist.isEmpty ? "(No Artist)" : artist
        }
    }
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 16)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.contentView.addSubview(nameLabel)
              
        NSLayoutConstraint.activate([
            nameLabel.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
            nameLabel.leftAnchor.constraint(equalTo: self.contentView.leftAnchor, constant: 6),
            nameLabel.rightAnchor.constraint(equalTo: self.contentView.rightAnchor, constant: -6),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
