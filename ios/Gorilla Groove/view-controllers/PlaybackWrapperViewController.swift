import Foundation
import UIKit

class PlaybackWrapperViewController: UIViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let stackView = UIStackView()
        stackView.axis = .vertical
        stackView.distribution  = .fill
        stackView.alignment = .fill

        let mediaControls = MediaControlsController()
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


