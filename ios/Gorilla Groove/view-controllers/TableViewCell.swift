import UIKit
import Foundation

class TableViewCell<T>: UITableViewCell {
    private let normalColor = UIColor.white
    private let selectionColor = UIColor(white: 0.85, alpha: 1)
    
    var data: T? = nil
    var tableIndex: Int = -1
    
    func animateSelectionColor() {
        UIView.animate(withDuration: 0.12, animations: {
            self.backgroundColor = self.selectionColor
        }) { (finished) in
            UIView.animate(withDuration: 0.12, animations: {
                self.backgroundColor = self.normalColor
            })
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.textLabel!.textColor = #colorLiteral(red: 0.1764705926, green: 0.4980392158, blue: 0.7568627596, alpha: 1)
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
}
