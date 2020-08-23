import UIKit
import Foundation

class TableViewCell<T>: UITableViewCell {

    var data: T? = nil
    var tableIndex: Int = -1

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.textLabel!.textColor = UIColor(named: "Table Text")
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}


extension UITableViewCell {
    func animateSelectionColor() {
        UIView.animate(withDuration: 0.12, animations: {
            self.backgroundColor = UIColor(named: "Table Transition")
        }) { (finished) in
            UIView.animate(withDuration: 0.12, animations: {
                self.backgroundColor = UIColor(named: "Background")
            })
        }
    }
}
