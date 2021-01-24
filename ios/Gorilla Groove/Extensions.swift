import Foundation
import UIKit

public extension Sequence {
    func groupBy<T: Hashable>(_ key: (Iterator.Element) -> T) -> [T:[Iterator.Element]] {
        return Dictionary.init(grouping: self, by: key)
    }
    
    func takeFirst(_ n: Int) -> [Iterator.Element] {
        // So inconvenient that prefix doesn't return an array...
        // Well whatever. Prefix is a dumb name for the function anyway given that they have "dropFirst" but don't use "takeFirst"
        return Array(self.prefix(n))
    }
}

/// Returns the element at the specified index if it is within bounds, otherwise nil.
extension Collection {
    subscript (safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

public extension Date {
    func toEpochTime() -> Int {
        return (Int(self.timeIntervalSince1970 * 1000))
    }
    
    static func minimum() -> Date {
        return 0.toDate()
    }
}

extension String {
    subscript(_ range: CountableRange<Int>) -> String {
        let start = index(startIndex, offsetBy: max(0, range.lowerBound))
        let end = index(startIndex, offsetBy: min(self.count, range.upperBound))
        return String(self[start..<end])
    }
    
    subscript(_ range: CountablePartialRangeFrom<Int>) -> String {
        let start = index(startIndex, offsetBy: max(0, range.lowerBound))
        return String(self[start...])
    }
    
    var boolValue: Bool {
        return (self as NSString).boolValue
    }
    
    func toTitleCase() -> String {
        return self.replacingOccurrences(of: "_", with: " ").capitalized
    }
    
    func index(from: Int) -> Index {
        return self.index(startIndex, offsetBy: from)
    }

    func substring(from: Int) -> String {
        let fromIndex = index(from: from)
        return String(self[fromIndex...])
    }

    func substring(to: Int) -> String {
        let toIndex = index(from: to)
        return String(self[..<toIndex])
    }

    func substring(with r: Range<Int>) -> String {
        let startIndex = index(from: r.lowerBound)
        let endIndex = index(from: r.upperBound)
        return String(self[startIndex..<endIndex])
    }
    
    func trim() -> String {
        return self.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

extension UIStackView {
    func addBackground(color: UIColor) {
        let subView = UIView(frame: bounds)
        subView.backgroundColor = color
        subView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        insertSubview(subView, at: 0)
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

extension UITextView {
    func scrollToBottom() {
        let textCount: Int = text.count
        guard textCount >= 1 else { return }
        scrollRangeToVisible(NSRange(location: textCount - 1, length: 1))
    }
}

extension NSRegularExpression {
    func matches(_ string: String) -> Bool {
        let range = NSRange(location: 0, length: string.utf16.count)
        return firstMatch(in: string, options: [], range: range) != nil
    }
}

extension FileManager {
    static func exists(_ path: URL) -> Bool {
        return FileManager.default.fileExists(atPath: path.path)
    }
    
    static func move(_ oldPath: URL, _ newPath: URL) {
        try! FileManager.default.moveItem(atPath: oldPath.path, toPath: newPath.path)
    }
}
