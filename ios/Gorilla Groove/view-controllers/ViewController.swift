import UIKit
import CoreData
import Foundation

struct VersionResponse: Codable {
    let version: String
}

struct LoginRequest: Encodable {
    let email: String
    let password: String
    let deviceId: String
    let deviceType: String = "IPHONE"
    let version: String = AppDelegate.getAppVersion()
}

class ViewController: UIViewController {    
    @IBOutlet weak var emailField: UITextField!
    @IBOutlet weak var passwordField: UITextField!
    @IBOutlet weak var appVersion: UILabel!
    @IBOutlet weak var apiVersion: UILabel!
    @IBOutlet weak var loginSpinner: UIActivityIndicatorView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let tap = UITapGestureRecognizer(
            target: self.view,
            action: #selector(UIView.endEditing)
        )
        
        view.addGestureRecognizer(tap)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        self.apiVersion.text = AppDelegate.getAppVersion()
    }
    
    private func presentLoggedInView(_ userId: Int) {
        Database.openDatabase(userId: userId)

        // If this is the first time this user has logged in, we want to sync in their library
        let user = UserState.getOwnUser()
        let newView: UIViewController = {
            if (user.lastSync == nil) {
                print("First time logging in with user \(user.name)")
                return SyncController()
            } else {
                print("Logged in with in with user \(user.name) that has already synced")
                return RootNavigationController()
            }
        }()

        // This isn't ideal because it isn't animated, but trying to present() or show() the view
        // was resulting in the view either not showing up, or having the incorrect size...
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        appDelegate.window!.rootViewController = newView
    }
    
    @IBAction func login(_ sender: Any) {
        // TODO check for empty inputs here maybe?
        // Might be able to use the "show()" function for a simple error message
        
        self.view.endEditing(false)
        
        let deviceId = FileState.read(DeviceState.self)!.deviceId
        let requestBody = LoginRequest(email: emailField.text!, password: passwordField.text!, deviceId: deviceId)
        
        loginSpinner.isHidden = false
        HttpRequester.post("authentication/login", LoginState.self, requestBody, authenticated: false) { response, statusCode, _ in
            if (statusCode != 200) {
                let message = statusCode == 403
                    ? "The login credentials are incorrect!"
                    : "Unable to contact Gorilla Groove. Try again in about a minute."
                DispatchQueue.main.async {
                    ViewUtil.showAlert(title: "Failed Login", message: message)
                    self.loginSpinner.isHidden = true
                }
                return
            }
            
            FileState.save(response!)
            UserState.isLoggedIn = true

            DispatchQueue.main.async {
                self.loginSpinner.isHidden = true
                
                self.presentLoggedInView(response!.id)
            }
        }
    }
}
