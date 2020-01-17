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
//                testNav.didMove(toParent: self)
    }
}

class MediaControls: UIViewController {
    override func viewDidLoad() {
        let av = AVPlayerViewController()
        av.player = AudioPlayer.shared.player
        
        self.view.addSubview(av.view)
        self.view.backgroundColor = .black
        self.view.translatesAutoresizingMaskIntoConstraints = false
        self.view.heightAnchor.constraint(equalToConstant: 120.0).isActive = true

        av.view.translatesAutoresizingMaskIntoConstraints = false
        av.view.heightAnchor.constraint(equalToConstant: 50).isActive = true
        av.view.widthAnchor.constraint(equalTo: self.view.widthAnchor).isActive = true
        av.view.topAnchor.constraint(equalTo: self.view.topAnchor).isActive = true
    }
}
