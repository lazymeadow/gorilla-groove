import UIKit
import Foundation

class TableFilter : UITableView, UITableViewDataSource, UITableViewDelegate {
    
    let filterOptions: [[FilterOption]]
    
    init(_ filterOptions: [[FilterOption]], vc: UIViewController) {
        self.filterOptions = filterOptions
        super.init(frame: .zero, style: .plain)
        
        self.translatesAutoresizingMaskIntoConstraints = false
        self.layer.borderWidth = 1
        self.layer.borderColor = Colors.inputLine.cgColor
        
        self.dataSource = self
        self.delegate = self
        self.register(TableFilterCell.self, forCellReuseIdentifier: "tableFilterCell")
        self.tableFooterView = UIView(frame: .zero)
        
        self.backgroundColor = Colors.background
        self.isHidden = true
        
        // Remove the left inset so the line goes all the way across
        self.separatorInset = UIEdgeInsets.init(top: 0, left: 0, bottom: 0, right: 0)
        self.separatorColor = Colors.tableText
        
        addFilterToNavigation(controller: vc)
        
        // Creating constraints to modify them later when content loads
        self.heightAnchor.constraint(equalToConstant: 0).isActive = true
        
        vc.view.addSubview(self)
        
        self.addObserver(self, forKeyPath: "contentSize", options: .new, context: nil)
    }
    
    private func addFilterToNavigation(
        controller: UIViewController
    ) {
        let config = UIImage.SymbolConfiguration(pointSize: UIFont.systemFontSize * 1.2, weight: .medium, scale: .large)
        let filterIcon = UIImage(systemName: "slider.horizontal.3", withConfiguration: config)!

        let filterItem = UIBarButtonItem(
            image: filterIcon,
            style: .plain,
            action: {
                self.setIsHiddenAnimated(!self.isHidden)
            }
        )
        
        var newNavItems = controller.navigationItem.rightBarButtonItems ?? []
        newNavItems.append(filterItem)
        controller.navigationItem.rightBarButtonItems = newNavItems
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return filterOptions[section].count
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return filterOptions.count
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        if section == 0 {
            return UIView(frame: .zero)
        }
        let view = UIView()
        view.backgroundColor = Colors.tableText

        return view
    }
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        if section == 0 {
            return 0
        } else {
            return 2
        }
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "tableFilterCell", for: indexPath) as! TableFilterCell
        let filterOption = filterOptions[indexPath.section][indexPath.row]

        cell.filterOption = filterOption
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
            
        return cell
    }
    
    @objc private func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! TableFilterCell
        cell.animateSelectionColor()
        
        let filterOption = cell.filterOption!

        GGNavLog.info("User tapped on filter option source with ID \(filterOption.name)")
        
        // At least for right now, I think it makes sense for all options to close this when they're tapped. If we don't want this
        // to happen, we could override this for individual entries later with a new property
        self.setIsHiddenAnimated(true)
        filterOption.onClick(filterOption)
    }
    
    // This function is a GIGANTIC CLUSTER. But I was struggling mad hard on not having constraint conflict warnings....
    // I need the table cells to have a width so I can manually set the table to have a width of the widest cell.
    // If I don't, the table has no width. However, I can't have a table cell be properly sized with constraints, because
    // table cells are constrained to be the size of the table cell. So there's this great chicken vs the egg problem
    // where I need the table cell width to know the size to set the table, but the table cells are constrained to the size
    // of the table. So instead, I manually check the size of the components of the table cell, and set the table to be
    // that combined size. Then the cells resize themselves and take up that width. It's BEAUTIFUL.
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if (keyPath == "contentSize"){
            guard let newValue = change?[.newKey] else { return }
            
            DispatchQueue.main.async {
                let newSize = newValue as! CGSize
                let heightConstraint = self.constraints.filter({ $0.firstAttribute == .height }).first!
                heightConstraint.constant = newSize.height
                
                let maxWidth = self.visibleCells.reduce(0.0) { (currentMax, cell) -> CGFloat in
                    let cell = cell as! TableFilterCell
                    let labelSize = cell.nameLabel.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).width
                    let checkmarkSize = cell.leftImage.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).width
                    
                    return max(labelSize + checkmarkSize, currentMax)
                }
                
                let rightMargin: CGFloat = 15
                
                if let existingWidthConstraint = self.constraints.filter({ $0.firstAttribute == .width }).first {
                    existingWidthConstraint.constant = maxWidth + rightMargin
                } else {
                    self.widthAnchor.constraint(equalToConstant: maxWidth + rightMargin).isActive = true
                }
            }
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class FilterOption {
    var name: String
    var filterImage: TableFilterImage
    var onClick: (FilterOption) -> Void
    
    init(
        _ name: String,
        filterImage: TableFilterImage = .NONE,
        onClick: @escaping (FilterOption) -> Void
    ) {
        self.name = name
        self.filterImage = filterImage
        self.onClick = onClick
    }
}

enum TableFilterImage {
    case CHECKED
    case ARROW_UP
    case ARROW_DOWN
    case NONE
}

fileprivate class TableFilterCell: UITableViewCell {
    
    private static let checkedIcon = IconView("checkmark", weight: .medium, scale: .medium)
    
    var filterOption: FilterOption? {
        didSet {
            guard let filterOption = filterOption else { return }
            nameLabel.text = filterOption.name
            nameLabel.sizeToFit()
            
            leftImage.isHidden = filterOption.filterImage == .NONE
            
            if filterOption.filterImage == .CHECKED {
                leftImage.changeImage("checkmark")
            } else if filterOption.filterImage == .ARROW_UP {
                leftImage.changeImage("arrow.up")
            } else if filterOption.filterImage == .ARROW_DOWN {
                leftImage.changeImage("arrow.down")
            }
        }
    }
    
    let nameLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 15)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    let leftImage: IconView = {
        let icon = IconView("checkmark", weight: .medium, scale: .medium)
        icon.translatesAutoresizingMaskIntoConstraints = false
        icon.tintColor = Colors.primary
        return icon
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.translatesAutoresizingMaskIntoConstraints = false
        
        self.contentView.addSubview(nameLabel)
        self.contentView.addSubview(leftImage)
        
        NSLayoutConstraint.activate([
            self.contentView.heightAnchor.constraint(equalTo: nameLabel.heightAnchor, constant: 19),
            
            leftImage.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor),
            leftImage.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
            
            nameLabel.leadingAnchor.constraint(equalTo: leftImage.trailingAnchor),
            nameLabel.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
