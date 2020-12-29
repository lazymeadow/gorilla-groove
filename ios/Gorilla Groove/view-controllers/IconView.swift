import Foundation
import UIKit

// I want a way to easily make consistent icons that don't grow / shrink when you place them into other views.
// The icon should ALWAYS be the same size / shape or else it gets stretched. So I added wrappers to these
// that will grow / shrink independently of the inner icon, that always retains its shape
class IconView : UIView {
    init(
        _ name: String,
        weight: UIImage.SymbolWeight = .ultraLight,
        scale: UIImage.SymbolScale = .small,
        multiplier: CGFloat = 1.5
    ) {
        super.init(frame: CGRect.zero)
                
        let image = SFIconCreator.create(name, weight: weight, scale: scale, multiplier: multiplier)
        let icon = UIImageView(image: image)
        
        let horizontalWrap = wrapView(icon, isWidth: true)
        let wrapper = wrapView(horizontalWrap, isWidth: false)
                
        self.addSubview(wrapper)
        
        self.widthAnchor.constraint(equalTo: wrapper.widthAnchor).isActive = true
        self.heightAnchor.constraint(equalTo: wrapper.heightAnchor).isActive = true
    }
    
    // Because UIEdgeInsets doesn't actually work no matter what I try, do this hacky nonsense instead
    private func wrapView(_ view: UIView, isWidth: Bool) -> UIView {
        let wrapper = UIStackView()
        wrapper.axis = isWidth ? .vertical : .horizontal
        wrapper.alignment = .center
        wrapper.distribution = .equalCentering
        wrapper.translatesAutoresizingMaskIntoConstraints = false

        wrapper.addArrangedSubview(view)

        wrapper.isUserInteractionEnabled = true
        
        if isWidth {
            wrapper.widthAnchor.constraint(equalTo: view.widthAnchor, constant: 20).isActive = true
        } else {
            wrapper.heightAnchor.constraint(equalTo: view.heightAnchor, constant: 20).isActive = true
        }
        
        return wrapper
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class SFIconCreator {
    static func create(
        _ name: String,
        weight: UIImage.SymbolWeight = .ultraLight,
        scale: UIImage.SymbolScale = .small,
        multiplier: CGFloat = 1.5
    ) -> UIImage {
        let config = UIImage.SymbolConfiguration(pointSize: UIFont.systemFontSize * multiplier, weight: weight, scale: scale)
        
        return UIImage(systemName: name, withConfiguration: config)!
    }
}
