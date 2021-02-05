import Foundation
import UIKit


class ViewUsersController : BaseUsersController<BasicEntityCell<User>> {

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        GGNavLog.info("Loaded view users controller")
    }
    
    override func handleTap(sender: UITapGestureRecognizer) {
        let cell = sender.view as! BasicEntityCell<User>
        
        cell.animateSelectionColor()
        
        let user = cell.entity!
        
        let vc = TrackViewController(user.name, originalView: .TITLE, user: user)
        
        vc.modalPresentationStyle = .fullScreen
        self.navigationController!.pushViewController(vc, animated: true)
    }
}
