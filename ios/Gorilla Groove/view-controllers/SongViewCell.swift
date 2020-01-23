//
//  SongViewCell.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/12/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import UIKit
import Foundation

class SongViewCell: UITableViewCell {
    static let normalColor = UIColor.white
    static let selectionColor = UIColor(white: 0.85, alpha: 1)
    
    var track:Track? {
        didSet {
            guard let track = track else {return}
            nameLabel.text = track.name

            if (track.artist.isEmpty) {
                artistLabel.text = ""
            } else {
                artistLabel.text = " \(track.artist) "
            }
        }
    }

    let containerView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.clipsToBounds = true // this will make sure its children do not go out of the boundary
        return view
    }()
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 20)
        label.textColor = .black
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let artistLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 14)
        label.textColor =  .white
        label.backgroundColor = #colorLiteral(red: 0.1764705926, green: 0.4980392158, blue: 0.7568627596, alpha: 1)
        label.layer.cornerRadius = 5
        label.clipsToBounds = true
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
        
        containerView.addSubview(nameLabel)
        containerView.addSubview(artistLabel)
        self.contentView.addSubview(containerView)
        self.backgroundColor = SongViewCell.normalColor
        
        containerView.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor).isActive = true
        containerView.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 10).isActive = true
        containerView.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor, constant: -10).isActive = true
        containerView.heightAnchor.constraint(equalToConstant:40).isActive = true
        
        nameLabel.topAnchor.constraint(equalTo: self.containerView.topAnchor).isActive = true
        nameLabel.leadingAnchor.constraint(equalTo: self.containerView.leadingAnchor).isActive = true
        nameLabel.trailingAnchor.constraint(equalTo: self.containerView.trailingAnchor).isActive = true
        
        artistLabel.topAnchor.constraint(equalTo: self.nameLabel.bottomAnchor).isActive = true
        artistLabel.leadingAnchor.constraint(equalTo: self.containerView.leadingAnchor).isActive = true
        artistLabel.topAnchor.constraint(equalTo: self.nameLabel.bottomAnchor).isActive = true
        artistLabel.leadingAnchor.constraint(equalTo: self.containerView.leadingAnchor).isActive = true
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
