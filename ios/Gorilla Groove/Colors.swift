import Foundation
import UIKit

class Colors {
    static let primary = UIColor("1D87BA")
    static let lightBlue = UIColor("99D5F1")
    static let aqua = UIColor("91F9E2")
    static let nearBlack = UIColor("333333")
    static let grey2 = UIColor("4F4F4F")
    static let grey3 = UIColor("828282")
    static let grey4 = UIColor("BDBDBD")
    
    private init() { }
}


extension UIColor {
    convenience init(_ hex: String) {
        let red = CGFloat(Int(hex[0..<2], radix: 16)!)
        let green = CGFloat(Int(hex[2..<4], radix: 16)!)
        let blue = CGFloat(Int(hex[4..<6], radix: 16)!)
        
        self.init(red: red / 255.0, green: green / 255.0, blue: blue / 255.0, alpha: 1)
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
}
