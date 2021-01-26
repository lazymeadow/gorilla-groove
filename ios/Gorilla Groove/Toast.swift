import Foundation
import UIKit

class Toast {
    static func show(_ message: String, view: UIView? = nil) {
        if Thread.isMainThread {
            (view ?? AppDelegate.rootView)?.makeToast(message)
        } else {
            DispatchQueue.main.async {
                (view ?? AppDelegate.rootView)?.makeToast(message)
            }
        }
    }
}
