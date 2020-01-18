import Foundation
import UIKit
import AVKit

class PlaybackWrapperViewController: UIViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let stackView = UIStackView()
        stackView.axis = NSLayoutConstraint.Axis.vertical
        stackView.distribution  = UIStackView.Distribution.fill
        stackView.alignment = UIStackView.Alignment.fill

        let mediaControls = MediaControls()
        let libraryController = MyLibraryController()

        let topNav = UINavigationController(rootViewController: libraryController)
        
        self.view.addSubview(stackView)
        self.addChild(topNav)

        stackView.addArrangedSubview(topNav.view)
        stackView.addArrangedSubview(mediaControls.view)
        stackView.translatesAutoresizingMaskIntoConstraints = false
        
        stackView.widthAnchor.constraint(equalTo: self.view.widthAnchor).isActive = true
        stackView.heightAnchor.constraint(equalTo: self.view.heightAnchor).isActive = true
    }
}

class MediaControls: UIViewController {
    let label = UILabel()
//    let paperPlane = UIImage(systemName: "paperplane.fill")
    
    override func viewDidLoad() {
        label.text = String(AudioPlayer.player.currentTime().seconds)
        label.sizeToFit()
        
        self.view.addSubview(label)
        self.view.backgroundColor = Colors.darkBlue
        self.view.translatesAutoresizingMaskIntoConstraints = false
        self.view.heightAnchor.constraint(equalToConstant: 100.0).isActive = true
        
        AudioPlayer.addTimeObserver { time in
            self.label.text = String(time)
            self.label.sizeToFit()
        }
    }
}

