import UIKit
import Foundation

class TableViewCell<T>: UITableViewCell {

    var data: T? = nil
    var tableIndex: Int = -1

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.textLabel!.textColor = Colors.tableText
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
