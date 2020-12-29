import Foundation

class Toast {
    static func show(_ message: String) {
        if Thread.isMainThread {
            AppDelegate.rootView?.makeToast(message)
        } else {
            DispatchQueue.main.async {
                AppDelegate.rootView?.makeToast(message)
            }
        }
    }
}
