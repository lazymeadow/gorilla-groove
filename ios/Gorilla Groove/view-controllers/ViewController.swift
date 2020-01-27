//
//  ViewController.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/1/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

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

struct LoginResponse: Codable {
    let token: String
    let id: Int
    //    let email: String
    let username: String
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
        
        getApiVersion()
    }
    
    private func presentLoggedInView() {
        // If this is the first time this user has logged in, we want to sync in their library
        let userSync = UserSyncManager().getLastSentUserSync()
        let newView: UIViewController = {
            if (userSync.last_sync == nil) {
                return SyncController()
            } else {
                return PlaybackWrapperViewController()
            }
        }()
        
        newView.modalPresentationStyle = .fullScreen
        newView.modalTransitionStyle = .crossDissolve
        self.present(newView, animated: true, completion: nil)
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
                    self.showAlert(message)
                    self.loginSpinner.isHidden = true
                }
                return
            }
            
            let decodedData = try! JSONDecoder().decode(LoginResponse.self, from: data!)
            LoginState.save(decodedData)
            DispatchQueue.main.async {
                self.loginSpinner.isHidden = true
                
                self.presentLoggedInView()
            }
        }
        dataTask.resume()
    }
    
    func getApiVersion() {
        print("Getting version")
        let session = URLSession(configuration: .default, delegate: nil, delegateQueue: .main)
        let url = URL(string: "https://gorillagroove.net/api/version")!
        let task = session.dataTask(with: url, completionHandler: { (data: Data?, response: URLResponse?, error: Error?) -> Void in
            let decodedData = try! JSONDecoder().decode(VersionResponse.self, from: data!)
            self.apiVersion.text = decodedData.version
        })
        task.resume()
    }
    
    func showAlert(_ message: String) {
        let alertController = UIAlertController(
            title: "Failed Login",
            message: message,
            preferredStyle: .alert
        )
        alertController.addAction(UIAlertAction(title: "Dismiss", style: .default))
        
        self.present(alertController, animated: true, completion: nil)
    }
    

}

