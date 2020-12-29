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

extension UIView {
    func animateTint(color: UIColor, timeInterval: Double = 0.12) {
        self.tintAdjustmentMode = .normal
        let startingColor = self.tintColor
        UIView.animate(withDuration: timeInterval, animations: {
            self.tintColor = color
        }) { (finished) in
            UIView.animate(withDuration: timeInterval, animations: {
                self.tintColor = startingColor
            })
        }
    }
    
    func animateSelectionColor() {
        animateBackgroundColor(color: Colors.tableTransition)
    }
    
    func animateBackgroundColor(color: UIColor, timeInterval: Double = 0.12) {
        let startingColor = self.backgroundColor
        UIView.animate(withDuration: 0.12, animations: {
            self.backgroundColor = color
        }) { (finished) in
            UIView.animate(withDuration: 0.12, animations: {
                self.backgroundColor = startingColor
            })
        }
    }
}
