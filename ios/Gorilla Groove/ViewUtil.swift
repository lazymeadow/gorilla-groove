import Foundation
import UIKit

class ViewUtil {
    static func showAlert(title: String? = nil, message: String? = nil, async: Bool = false) {
        let alertController = UIAlertController(
            title: title,
            message: message,
            preferredStyle: .alert
        )
        alertController.addAction(UIAlertAction(title: "Dismiss", style: .default))
        
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        let rootVc = appDelegate.window!.rootViewController!
        
        if async {
            DispatchQueue.main.async {
                rootVc.present(alertController, animated: true, completion: nil)
            }
        } else {
            rootVc.present(alertController, animated: true, completion: nil)
        }
    }
}
