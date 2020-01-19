import UIKit
import SwiftUI
import Foundation

class MediaControlsController: UIViewController {
    let label = UILabel()
    
    var repeatIcon: UIImageView { return createIcon("repeat", weight: .bold) }
    var backIcon: UIImageView { return createIcon("backward.end.fill") }
    var rewindIcon: UIImageView { return createIcon("backward.fill") }
    var playIcon: UIImageView { return createIcon("play.fill", scale: .large) }
    var pauseIcon: UIImageView { return createIcon("pause.fill") }
    var fastForwardIcon: UIImageView { return createIcon("forward.fill") }
    var forwardIcon: UIImageView { return createIcon("forward.end.fill") }
    var shuffleIcon: UIImageView { return createIcon("shuffle", weight: .bold) }
    
    var songText: UILabel {
        let label = UILabel()
        
        label.textColor = .white
        label.text = "Song - Artist"
        label.font = label.font.withSize(12)
        label.sizeToFit()
        
        return label
    }
    
    var currentTime: UILabel {
        let label = UILabel()
        
        label.textColor = .white
        label.text = "1:23"
        label.font = label.font.withSize(12)
        label.sizeToFit()
        
        return label
    }
    
    var totalTime: UILabel {
        let label = UILabel()
        
        label.textColor = .white
        label.text = "4:24"
        label.font = label.font.withSize(12)
        label.sizeToFit()
        
        return label
    }
    
    override func viewDidLoad() {
        let content = UIStackView()
        content.translatesAutoresizingMaskIntoConstraints = false
        content.axis = .vertical
        content.alignment = .center
        
        let topButtons = createTopButtons()
        let songTextView = createSongText()
        let bottomElements = createBottomElements()

        content.addArrangedSubview(topButtons)
        content.addArrangedSubview(songTextView)
        content.addArrangedSubview(bottomElements)
        
        content.setCustomSpacing(8.0, after: topButtons)
        content.setCustomSpacing(8.0, after: songTextView)
        
        self.view.addSubview(content)
        self.view.backgroundColor = Colors.darkBlue
        self.view.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            self.view.heightAnchor.constraint(equalToConstant: 90.0),
            
            content.topAnchor.constraint(equalTo: self.view.topAnchor, constant: 10),
            content.leftAnchor.constraint(equalTo: self.view.leftAnchor),
            content.rightAnchor.constraint(equalTo: self.view.rightAnchor),
            
            topButtons.leftAnchor.constraint(equalTo: content.leftAnchor, constant: 10),
            topButtons.rightAnchor.constraint(equalTo: content.rightAnchor, constant: -10),
            
            bottomElements.leftAnchor.constraint(equalTo: content.leftAnchor, constant: 10),
            bottomElements.rightAnchor.constraint(equalTo: content.rightAnchor, constant: -10)
        ])

        AudioPlayer.addTimeObserver { time in
            self.label.text = String(time)
            self.label.sizeToFit()
        }
    }
    
    private func createTopButtons() -> UIStackView {
        let buttons = UIStackView()
        buttons.translatesAutoresizingMaskIntoConstraints = false
        buttons.axis = .horizontal
        buttons.distribution  = .equalSpacing
        
        let topMiddle = UIStackView()
        topMiddle.translatesAutoresizingMaskIntoConstraints = false
        topMiddle.axis = .horizontal
        topMiddle.distribution  = .equalSpacing

        topMiddle.addArrangedSubview(backIcon)
        topMiddle.addArrangedSubview(rewindIcon)
        topMiddle.addArrangedSubview(playIcon)
        topMiddle.addArrangedSubview(fastForwardIcon)
        topMiddle.addArrangedSubview(forwardIcon)
                
        buttons.addArrangedSubview(repeatIcon)
        buttons.addArrangedSubview(topMiddle)
        buttons.addArrangedSubview(shuffleIcon)
        
        topMiddle.widthAnchor.constraint(equalTo: buttons.widthAnchor, constant: -130).isActive = true

        return buttons
    }
    
    private func createSongText() -> UIView {
        let view = UIStackView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.addArrangedSubview(songText)
        
        return view
    }
    
    private func createBottomElements() -> UIStackView {
        let elements = UIStackView()
        elements.translatesAutoresizingMaskIntoConstraints = false
        elements.axis = .horizontal
        elements.distribution  = .fill

        let slider = UISlider()
        slider.maximumValue = 100
        slider.minimumValue = 0
        slider.setValue(50, animated: false)
        
        slider.minimumTrackTintColor = Colors.lightBlue
        slider.maximumTrackTintColor = Colors.nearBlack
        slider.thumbTintColor = Colors.lightBlue

        let config = UIImage.SymbolConfiguration(scale: .small)
        let newImage = UIImage(systemName: "circle.fill", withConfiguration: config)?.tinted(color: Colors.lightBlue)

        slider.setThumbImage(newImage, for: .normal)
        slider.setThumbImage(newImage, for: .highlighted)
                
        elements.addArrangedSubview(currentTime)
        elements.addArrangedSubview(slider)
        elements.addArrangedSubview(totalTime)

        // Kind of jank, but I want the slider to grow, and leave space for both sides
        // For whatever reason, custom spacing after the leading text wasn't being obeyed,
        // and just using normal left / right constraints threw crytic runtime exceptions.
        // This seems to work though, and is evenly spaced on both the iPhone 8 and 11 Pro Max
        slider.widthAnchor.constraint(equalTo: elements.widthAnchor, constant: -80).isActive = true
        elements.setCustomSpacing(15.0, after: slider)

        return elements
    }
    
    private func createIcon(
        _ name: String,
        pointSize: Double = 1.5,
        weight: UIImage.SymbolWeight = .ultraLight,
        scale: UIImage.SymbolScale = .small
    ) -> UIImageView {
        let config = UIImage.SymbolConfiguration(pointSize: UIFont.systemFontSize * 1.5, weight: weight, scale: scale)

        let icon = UIImageView(image: UIImage(systemName: name, withConfiguration: config)!)
        icon.tintColor = .white
        
        return icon
    }
}

extension UIImage {
    // I stole this and converted it from Objective-C
    // https://coffeeshopped.com/2010/09/iphone-how-to-dynamically-color-a-uiimage
    func tinted(color: UIColor) -> UIImage {
        UIGraphicsBeginImageContext(size)

        let context = UIGraphicsGetCurrentContext()!

        context.setFillColor(color.cgColor)
        context.translateBy(x: 0, y: size.height)
        context.scaleBy(x: 1.0, y: -1.0);

        context.setBlendMode(CGBlendMode.normal)
        let rect = CGRect(x: 0, y: 0, width: size.width, height: size.height)
        context.draw(self.cgImage!, in: rect)

        context.clip(to: rect, mask: self.cgImage!);
        context.addRect(rect);
        context.drawPath(using: CGPathDrawingMode.fill)

        let coloredImg = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();

        return coloredImg!
    }
}
