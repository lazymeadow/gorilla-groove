import UIKit
import Foundation

class AlbumViewCell: UITableViewCell {
    
    var album: Album? {
        didSet {
            guard let album = album else {return}
            nameLabel.text = album.name.isEmpty ? "(No Album)" : album.name
            artView.image = album.art
        }
    }
    
    var artView: UIImageView = {
        let view = UIImageView()
        view.contentMode = .scaleAspectFit
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 16)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    var tableIndex: Int = -1

    override func prepareForReuse() {
        super.prepareForReuse()
        
        artView.image = nil
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        let containerView = UIStackView()
        containerView.axis = .horizontal
        containerView.distribution  = .fill
        containerView.alignment = .center
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        containerView.addArrangedSubview(artView)
        containerView.addArrangedSubview(nameLabel)
        
        self.contentView.addSubview(containerView)
        
        containerView.setCustomSpacing(4.0, after: artView)
        
        NSLayoutConstraint.activate([
            containerView.heightAnchor.constraint(equalTo: self.contentView.heightAnchor),
            containerView.leftAnchor.constraint(equalTo: self.contentView.leftAnchor, constant: 6),
            containerView.rightAnchor.constraint(equalTo: self.contentView.rightAnchor, constant: -6),
            
            artView.widthAnchor.constraint(equalToConstant: 40),
            artView.heightAnchor.constraint(equalToConstant: 40),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
