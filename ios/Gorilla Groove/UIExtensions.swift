import UIKit
import Foundation

private var actionKey: Void?

// What a legend https://stackoverflow.com/a/47159432/13175115
extension UIBarButtonItem {

    private var _action: () -> () {
        get {
            return objc_getAssociatedObject(self, &actionKey) as! () -> ()
        }
        set {
            objc_setAssociatedObject(self, &actionKey, newValue, objc_AssociationPolicy.OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    convenience init(image: UIImage?, style: UIBarButtonItem.Style, action: @escaping () -> ()) {
        self.init(image: image, style: style, target: nil, action: #selector(pressed))
        self.target = self
        self._action = action
    }

    convenience init(title: String, style: UIBarButtonItem.Style, action: @escaping () -> ()) {
        self.init(title: title, style: style, target: nil, action: #selector(pressed))
        self.target = self
        self._action = action
    }
    
    @objc private func pressed(sender: UIBarButtonItem) {
        _action()
    }
}

extension UIView {
    var firstResponder: UIView? {
        guard !isFirstResponder else { return self }

        for subview in subviews {
            if let firstResponder = subview.firstResponder {
                return firstResponder
            }
        }

        return nil
    }
    
    func setIsHiddenAnimated(_ hidden: Bool) {
        if hidden == isHidden {
            return
        }
        
        if !hidden {
            self.alpha = 0.0
            self.isHidden = false
        }
        UIView.animate(withDuration: 0.20, animations: {
            self.alpha = hidden ? 0.0 : 1.0
        }) { (complete) in
            self.isHidden = hidden
        }
    }
}

extension UIViewController {
    var searchText: String {
        get {
            guard let searchController = self.navigationItem.searchController else {
                return ""
            }
            
            return searchController.searchBar.text ?? ""
        }
        set {
            guard let searchController = self.navigationItem.searchController else {
                GGLog.warning("Tried to set the search text of a non-existent search controller")
                return
            }
            
            return searchController.searchBar.text = newValue
        }
    }
    
    func cancelSearch() {
        guard let searchBar = self.navigationItem.searchController?.searchBar else {
            // Already not searching
            return
        }
        
        searchBar.delegate!.searchBarCancelButtonClicked!(searchBar)
    }
    
    var isActiveVc: Bool {
        get {
            return viewIfLoaded?.window != nil
        }
    }
}
