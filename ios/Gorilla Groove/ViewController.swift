//
//  ViewController.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/1/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import UIKit
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
    //    let email: String
    let username: String
}

class ViewController: UIViewController {
    
    @IBOutlet weak var emailField: UITextField!
    @IBOutlet weak var passwordField: UITextField!
    @IBOutlet weak var appVersion: UILabel!
    @IBOutlet weak var apiVersion: UILabel!
    @IBOutlet weak var loginSpinner: UIActivityIndicatorView!
    @IBOutlet weak var loginToken: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        print("hello, console!")
        getApiVersion()
        loginToken.text = readLoginState()
        
        
        let tap = UITapGestureRecognizer(
            target: self.view,
            action: #selector(UIView.endEditing)
        )
        
        view.addGestureRecognizer(tap)
    }
    
    @IBAction func login(_ sender: Any) {
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
            self.saveLoginState(decodedData)
            DispatchQueue.main.async {
                self.loginSpinner.isHidden = true
                self.loginToken.text = decodedData.token
            }
        }
        dataTask.resume()
    }
    
    func getApiVersion() {
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
    
    func saveLoginState(_ loginResponse: LoginResponse) {
        let encoder = PropertyListEncoder()
        encoder.outputFormat = .xml
        
        let path = getPlistPath()
        
        print("Saving to path")
        print(path)
        do {
            let data = try encoder.encode(loginResponse)
            try data.write(to: URL(fileURLWithPath: path))
        } catch {
            print(error)
        }
    }
    
    func readLoginState() -> String? {
        print("Read state")
        let path = getPlistPath()
        print(path)
        
        let xml = FileManager.default.contents(atPath: path)
        if (xml == nil) {
            return nil
        }
        let savedState = try? PropertyListDecoder().decode(LoginResponse.self, from: xml!)
        
        return savedState?.token
    }
    
    private func getPlistPath() -> String {
        let plistFileName = "data.plist"
        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        let documentPath = paths[0] as NSString
        let plistPath = documentPath.appendingPathComponent(plistFileName)
        return plistPath
    }
}

