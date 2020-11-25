import Foundation
import UIKit

class ViewUtil {
    static func showAlert(
        title: String? = nil,
        message: String? = nil,
        yesText: String? = nil,
        yesStyle: UIAlertAction.Style = .default,
        dismissText: String = "Dismiss",
        yesAction: (() -> Void)? = nil
    ) {
        // APPARENTLY if you CREATE a UIAlertController without being on the main thread, shit gets mad at you. I have no idea
        // why it is important to be on the main thread to create a controller that has not yet been put into the UI.... thanks Apple.
        // Error: "[Assert] Cannot be called with asCopy = NO on non-main thread."
        if Thread.isMainThread {
            showAlertInternal(title: title, message: message, yesText: yesText, yesStyle: yesStyle, dismissText: dismissText, yesAction: yesAction)
        } else {
            DispatchQueue.main.async {
                showAlertInternal(title: title, message: message, yesText: yesText, yesStyle: yesStyle, dismissText: dismissText, yesAction: yesAction)
            }
        }
    }
    
    private static func showAlertInternal(
        title: String? = nil,
        message: String? = nil,
        yesText: String? = nil,
        yesStyle: UIAlertAction.Style = .default,
        dismissText: String = "Dismiss",
        yesAction: (() -> Void)? = nil
    ) {
        let alertController = UIAlertController(
            title: title,
            message: message,
            preferredStyle: .alert
        )
        if let yesText = yesText {
            alertController.addAction(UIAlertAction(title: yesText, style: yesStyle, handler: { _ in
                yesAction!()
            }))
        }
        alertController.addAction(UIAlertAction(title: dismissText, style: .default))
        
        showAlert(alertController)
    }
    
    static func showAlert(_ alertController: UIAlertController) {
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        let rootVc = appDelegate.window!.rootViewController!
        
        rootVc.presentedViewController?.dismiss(animated: true, completion: {
            GGLog.debug("Dismissed existing alert")
        })
        
        if Thread.isMainThread {
            rootVc.present(alertController, animated: true)
        } else {
            DispatchQueue.main.async {
                rootVc.present(alertController, animated: true)
            }
        }
    }
}
