//
//  Colors.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/18/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import Foundation
import UIKit

class Colors {
    static let darkBlue = UIColor("1D87BA")
    
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
