import Foundation
import UIKit

class GGActionSheet {

    static func create() -> UIAlertController {
        // iPads crash if you present with .actionSheet style. lol? Ok Apple? Super cool?
        var alertStyle = UIAlertController.Style.actionSheet
        if (UIDevice.current.userInterfaceIdiom == .pad) {
            alertStyle = UIAlertController.Style.alert
        }
        
        return UIAlertController(title: nil, message: nil, preferredStyle: alertStyle)
    }
}
