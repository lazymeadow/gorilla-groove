import Foundation
import UIKit

// Heavily taken from https://medium.com/swift-india/simple-checkbox-component-in-ios-311a865c0b02
class GGCheckbox : UIButton {
    
    var isChecked = false {
        didSet {
            self.setNeedsDisplay()
        }
    }
    
    private let borderWidth: CGFloat = 1.75
    
    public init(size: CGFloat) {
        super.init(frame: .zero)
        self.backgroundColor = .clear
        self.translatesAutoresizingMaskIntoConstraints = false
        
        self.widthAnchor.constraint(equalToConstant: size).isActive = true
        self.heightAnchor.constraint(equalToConstant: size).isActive = true
    }
    
    open override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.isChecked = !isChecked
        self.sendActions(for: .valueChanged)
    }
    
    open override func draw(_ rect: CGRect) {
        let newRect = rect.insetBy(dx: borderWidth / 2, dy: borderWidth / 2)
        
        let context = UIGraphicsGetCurrentContext()!
        context.setStrokeColor(Colors.tableText.cgColor)
        context.setLineWidth(borderWidth)
        
        let shapePath = UIBezierPath(roundedRect: newRect, cornerRadius: 4)
        
        context.addPath(shapePath.cgPath)
        context.strokePath()
        context.fillPath()
        
        if isChecked {
            self.drawCheckMark(frame: newRect)
        }
    }
    
    private func drawCheckMark(frame: CGRect) {
        let bezierPath = UIBezierPath()
        bezierPath.move(to: CGPoint(x: frame.minX + 0.26000 * frame.width, y: frame.minY + 0.50000 * frame.height))
        bezierPath.addCurve(to: CGPoint(x: frame.minX + 0.42000 * frame.width, y: frame.minY + 0.62000 * frame.height), controlPoint1: CGPoint(x: frame.minX + 0.38000 * frame.width, y: frame.minY + 0.60000 * frame.height), controlPoint2: CGPoint(x: frame.minX + 0.42000 * frame.width, y: frame.minY + 0.62000 * frame.height))
        bezierPath.addLine(to: CGPoint(x: frame.minX + 0.70000 * frame.width, y: frame.minY + 0.24000 * frame.height))
        bezierPath.addLine(to: CGPoint(x: frame.minX + 0.78000 * frame.width, y: frame.minY + 0.30000 * frame.height))
        bezierPath.addLine(to: CGPoint(x: frame.minX + 0.44000 * frame.width, y: frame.minY + 0.76000 * frame.height))
        bezierPath.addCurve(to: CGPoint(x: frame.minX + 0.20000 * frame.width, y: frame.minY + 0.58000 * frame.height), controlPoint1: CGPoint(x: frame.minX + 0.44000 * frame.width, y: frame.minY + 0.76000 * frame.height), controlPoint2: CGPoint(x: frame.minX + 0.26000 * frame.width, y: frame.minY + 0.62000 * frame.height))
        Colors.tableText.setFill()
        bezierPath.fill()
    }
    
    open override func layoutSubviews() {
        super.layoutSubviews()
        self.setNeedsDisplay()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
