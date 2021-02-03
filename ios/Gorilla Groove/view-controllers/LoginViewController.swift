import Foundation
import UIKit

class LoginViewController: UIViewController {
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        
        label.text = "Gorilla Groove"
        label.textColor = Colors.foreground
        label.font = label.font.withSize(26)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
                
        return label
    }()
    
    private lazy var emailField: UITextField = {
        let field = UITextField()
        field.font = field.font!.withSize(18)
        
        let placeholderText = "Email"
        field.attributedPlaceholder = NSAttributedString(
            string: placeholderText,
            attributes: [NSAttributedString.Key.foregroundColor : Colors.inputLine]
        )
        
        field.autocorrectionType = .no
        field.autocapitalizationType = .none
        field.translatesAutoresizingMaskIntoConstraints = false
        field.textContentType = .emailAddress
        field.delegate = self
        field.tintColor = Colors.tableText
        
        return field
    }()
    
    private lazy var emailBottomLine: UIView = {
        let field = UIView()
        
        field.translatesAutoresizingMaskIntoConstraints = false
        field.backgroundColor = Colors.inputLine

        return field
    }()
    
    private lazy var passwordField: UITextField = {
        let field = UITextField()
        field.font = field.font!.withSize(18)
        
        let placeholderText = "Password"
        field.attributedPlaceholder = NSAttributedString(
            string: placeholderText,
            attributes: [NSAttributedString.Key.foregroundColor : Colors.inputLine]
        )
        
        field.translatesAutoresizingMaskIntoConstraints = false
        field.delegate = self
        field.textContentType = .password
        field.isSecureTextEntry = true

        return field
    }()
    
    private lazy var passwordBottomLine: UIView = {
        let field = UIView()
        
        field.translatesAutoresizingMaskIntoConstraints = false
        field.backgroundColor = Colors.inputLine
        
        return field
    }()
    
    private let appVersion: UILabel = {
        let label = UILabel()
        
        label.textColor = Colors.foreground
        label.font = label.font.withSize(14)
        label.translatesAutoresizingMaskIntoConstraints = false
        label.sizeToFit()
        
        return label
    }()
    
    private let submitButton: GGButton = {
        let button = GGButton()
        button.setTitle("Download", for: .normal)
        
        return button
    }()
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = true
        spinner.color = Colors.foreground
        
        return spinner
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.backgroundColor = Colors.background
        
        view.addSubview(titleLabel)
        view.addSubview(emailField)
        view.addSubview(emailBottomLine)
        view.addSubview(passwordField)
        view.addSubview(passwordBottomLine)
        view.addSubview(appVersion)
        view.addSubview(submitButton)
        view.addSubview(activitySpinner)

        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            titleLabel.topAnchor.constraint(equalTo: view.topAnchor, constant: 80),
            
            emailField.widthAnchor.constraint(equalToConstant: 250),
            emailField.heightAnchor.constraint(equalToConstant: 50),
            emailField.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            emailField.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -90),
            
            emailBottomLine.leadingAnchor.constraint(equalTo: emailField.leadingAnchor, constant: -20),
            emailBottomLine.trailingAnchor.constraint(equalTo: emailField.trailingAnchor, constant: 20),
            emailBottomLine.heightAnchor.constraint(equalToConstant: 1),
            emailBottomLine.topAnchor.constraint(equalTo: emailField.bottomAnchor, constant: 0),
            
            passwordField.widthAnchor.constraint(equalToConstant: 250),
            passwordField.heightAnchor.constraint(equalToConstant: 50),
            passwordField.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            passwordField.topAnchor.constraint(equalTo: emailBottomLine.bottomAnchor, constant: 40),
            
            passwordBottomLine.leadingAnchor.constraint(equalTo: passwordField.leadingAnchor, constant: -20),
            passwordBottomLine.trailingAnchor.constraint(equalTo: passwordField.trailingAnchor, constant: 20),
            passwordBottomLine.heightAnchor.constraint(equalToConstant: 1),
            passwordBottomLine.topAnchor.constraint(equalTo: passwordField.bottomAnchor, constant: 0),
            
            submitButton.leadingAnchor.constraint(equalTo: passwordBottomLine.leadingAnchor),
            submitButton.trailingAnchor.constraint(equalTo: passwordBottomLine.trailingAnchor),
            submitButton.heightAnchor.constraint(equalToConstant: 50),
            submitButton.topAnchor.constraint(equalTo: passwordBottomLine.bottomAnchor, constant: 80),
            
            appVersion.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 6),
            appVersion.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -6),
            
            activitySpinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            activitySpinner.centerYAnchor.constraint(equalTo: view.safeAreaLayoutGuide.centerYAnchor),
        ])
        
        let tap = UITapGestureRecognizer(
            target: self.view,
            action: #selector(UIView.endEditing)
        )
        
        view.addGestureRecognizer(tap)
        
        submitButton.addTarget(self, action: #selector(login), for: .touchUpInside)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        appVersion.text = AppDelegate.getAppVersion()
        appVersion.sizeToFit()
    }
    
    private func presentLoggedInView(_ userId: Int) {
        Database.openDatabase(userId: userId)

        // If this is the first time this user has logged in, we want to sync in their library
        let newView: UIViewController = {
            if (ServerSynchronizer.lastSync == Date.minimum()) {
                GGLog.info("First time logging in")
                return SyncController()
            } else {
                GGLog.info("Logged in with in with user that has already synced")
                return RootNavigationController()
            }
        }()

        // This isn't ideal because it isn't animated, but trying to present() or show() the view
        // was resulting in the view either not showing up, or having the incorrect size...
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        appDelegate.window!.rootViewController = newView
    }
    
    @objc private func login() {
        if emailField.text!.isEmpty {
            ViewUtil.showAlert(title: "Email Missing", message: "Please fill out the email field")
            return
        }
        
        if passwordField.text!.isEmpty {
            ViewUtil.showAlert(title: "Password Missing", message: "Please fill out the password field")
            return
        }
        
        self.view.endEditing(false)
        
        let deviceId = FileState.read(DeviceState.self)!.deviceId
        let requestBody = LoginRequest(
            email: emailField.text!,
            password: passwordField.text!,
            deviceId: deviceId,
            preferredDeviceName: UIDevice.current.name
        )
        
        activitySpinner.startAnimating()
        submitButton.isDisabled = true
        
        let this = self
        HttpRequester.post("authentication/login", LoginState.self, requestBody, authenticated: false) { response, statusCode, _ in
            if (statusCode != 200) {
                let message = statusCode == 403
                    ? "The login credentials are incorrect!"
                    : "Unable to contact Gorilla Groove. Try again in about a minute."
                DispatchQueue.main.async {
                    ViewUtil.showAlert(title: "Failed Login", message: message)
                    this.activitySpinner.stopAnimating()
                    this.submitButton.isDisabled = false
                }
                return
            }
            
            FileState.save(response!)
            UserState.isLoggedIn = true

            DispatchQueue.main.async {
                this.activitySpinner.stopAnimating()
                this.submitButton.isDisabled = false

                this.presentLoggedInView(response!.id)
            }
        }
    }
    
    init() {
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

extension LoginViewController: UITextFieldDelegate {
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        login()
        textField.resignFirstResponder()
        return true
    }
}


struct VersionResponse: Codable {
    let version: String
}

struct LoginRequest: Encodable {
    let email: String
    let password: String
    let deviceId: String
    let deviceType: String = "IPHONE"
    let preferredDeviceName: String
    let version: String = AppDelegate.getAppVersion()
}
