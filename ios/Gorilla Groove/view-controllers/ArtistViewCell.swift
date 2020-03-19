import UIKit
import Foundation

class ArtistViewCell: UITableViewCell {
    
    var artist: String? {
        didSet {
            guard let artist = artist else {return}
            nameLabel.text = artist.isEmpty ? "(No Artist)" : artist
        }
    }
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 16)
        label.textColor = Colors.grey2
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        let containerView = UIStackView()
        containerView.axis = .horizontal
        containerView.distribution  = .fill
        containerView.alignment = .center
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        containerView.addArrangedSubview(nameLabel)
        
        self.contentView.addSubview(containerView)
                
        let constraint = self.contentView.heightAnchor.constraint(equalToConstant: 40)
        constraint.priority = UILayoutPriority(750)
        constraint.isActive = true

        NSLayoutConstraint.activate([
            containerView.heightAnchor.constraint(equalTo: self.contentView.heightAnchor),
            containerView.leftAnchor.constraint(equalTo: self.contentView.leftAnchor, constant: 6),
            containerView.rightAnchor.constraint(equalTo: self.contentView.rightAnchor, constant: -6),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
