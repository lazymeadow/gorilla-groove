import UIKit
import CoreData
import Foundation

struct VersionResponse: Codable {
    let version: String
}

struct LoginRequest: Codable {
    let email: String
    let password: String
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
    
    private func presentLoggedInView() {
        // If this is the first time this user has logged in, we want to sync in their library
        let user = UserState.getOwnUser()
        let newView: UIViewController = {
            if (user.lastSync == nil) {
                return SyncController()
            } else {
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
        
        let session = URLSession(configuration: .default)
        let url = URL(string: "https://gorillagroove.net/api/authentication/login")
        var request : URLRequest = URLRequest(url: url!)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let requestBody = LoginRequest(email: emailField.text!, password: passwordField.text!)
        let requestJson = try! JSONEncoder().encode(requestBody)
        
        request.httpBody = requestJson
        loginSpinner.isHidden = false
        let dataTask = session.dataTask(with: request) { data, response, error in
            guard let httpResponse = response as? HTTPURLResponse
                else {
                    print("error: not a valid http response")
                    return
            }
            if (httpResponse.statusCode != 200) {
                let message = httpResponse.statusCode == 403
                    ? "The login credentials are incorrect!"
                    : "Unable to contact Gorilla Groove. Try again in a minute."
                DispatchQueue.main.async {
                    ViewUtil.showAlert(title: "Failed Login", message: message)
                    self.loginSpinner.isHidden = true
                }
                return
            }
            
            let decodedData = try! JSONDecoder().decode(LoginState.self, from: data!)
            FileState.save(decodedData)

            DispatchQueue.main.async {
                self.loginSpinner.isHidden = true
                
                self.presentLoggedInView()
            }
        }
        dataTask.resume()
    }
}
