import UIKit
import Foundation

class AlbumViewCell: UITableViewCell {
    
    var album: Album? {
        didSet {
            guard let album = album else {return}
            nameLabel.text = album.name.isEmpty ? " " : album.name
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
        label.textColor = Colors.grey2
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
            containerView.heightAnchor.constraint(equalTo: self.contentView.heightAnchor, constant: -4),
            containerView.leftAnchor.constraint(equalTo: self.contentView.leftAnchor, constant: 6),
            containerView.rightAnchor.constraint(equalTo: self.contentView.rightAnchor, constant: -6),
            
            artView.widthAnchor.constraint(equalToConstant: 32),
            artView.heightAnchor.constraint(equalToConstant: 32),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
