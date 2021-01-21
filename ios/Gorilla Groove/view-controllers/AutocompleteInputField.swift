import Foundation
import UIKit


class AutoCompleteInputField : UIView, UITableViewDataSource, UITableViewDelegate {
    private let autoCompleteTable: UITableView = {
        let table = UITableView()
        table.translatesAutoresizingMaskIntoConstraints = false
        table.layer.borderWidth = 1
        table.layer.borderColor = Colors.inputLine.cgColor
        
        return table
    }()
    var autoCompleteData: [String] = [] {
        didSet {
            autoCompleteTable.reloadData()
        }
    }
    
    let textField: UITextField = {
        let field = UITextField()
        field.translatesAutoresizingMaskIntoConstraints = false
        
        return field
    }()
    
    var loading: Bool = false {
        didSet {
            if loading {
                activitySpinner.startAnimating()
            } else {
                activitySpinner.stopAnimating()
            }
        }
    }
    
    var text: String {
        get {
            textField.text ?? ""
        }
    }
    
    private let activitySpinner: UIActivityIndicatorView = {
        let spinner = UIActivityIndicatorView()
        
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = true
        spinner.color = Colors.foreground
        
        return spinner
    }()
    
    @discardableResult
    override func resignFirstResponder() -> Bool {
        super.resignFirstResponder()
        self.textField.resignFirstResponder()
        
        return true
    }
    
    var textChangeHandler: ((_ newText: String) -> Void)? = nil
    var submitHandler: ((_ text: String) -> Void)? = nil
    
    init() {
        super.init(frame: .zero)
        
        self.addSubview(textField)
        self.addSubview(autoCompleteTable)
        self.addSubview(activitySpinner)
        
        self.translatesAutoresizingMaskIntoConstraints = false
                
        autoCompleteTable.dataSource = self
        autoCompleteTable.delegate = self
        autoCompleteTable.register(AutocompleteCell.self, forCellReuseIdentifier: "autocompleteCell")
        autoCompleteTable.tableFooterView = UIView(frame: .zero)

        NSLayoutConstraint.activate([
            textField.trailingAnchor.constraint(equalTo: self.trailingAnchor),
            textField.leadingAnchor.constraint(equalTo: self.leadingAnchor),
            textField.topAnchor.constraint(equalTo: self.topAnchor),
            
            self.bottomAnchor.constraint(equalTo: autoCompleteTable.bottomAnchor),
            
            autoCompleteTable.trailingAnchor.constraint(equalTo: textField.trailingAnchor),
            autoCompleteTable.leadingAnchor.constraint(equalTo: textField.leadingAnchor),
            autoCompleteTable.topAnchor.constraint(equalTo: textField.bottomAnchor, constant: 15),
            autoCompleteTable.heightAnchor.constraint(equalToConstant: 0), // Creating constraint to modify it later
            
            activitySpinner.trailingAnchor.constraint(equalTo: textField.trailingAnchor),
            activitySpinner.centerYAnchor.constraint(equalTo: textField.centerYAnchor),
        ])
        
        self.autoCompleteTable.addObserver(self, forKeyPath: "contentSize", options: .new, context: nil)
        textField.addTarget(self, action: #selector(textFieldDidChange), for: .editingChanged)
        textField.addTarget(self, action: #selector(submitCalled), for: .primaryActionTriggered)
    }
    
    @objc private func textFieldDidChange() {
        textChangeHandler?(textField.text ?? "")
    }
    
    @objc private func submitCalled() {
        submitHandler?(textField.text ?? "")
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
         if (keyPath == "contentSize"){
            if let newValue = change?[.newKey] {
                DispatchQueue.main.async {
                    let newSize  = newValue as! CGSize
                    let heightConstraint = self.autoCompleteTable.constraints.filter({ $0.firstAttribute == .height }).first!
                    heightConstraint.constant = newSize.height
                }
             }
         }
     }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return autoCompleteData.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "autocompleteCell", for: indexPath) as! AutocompleteCell
        cell.autocompleteText = autoCompleteData[indexPath.row]
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        cell.addGestureRecognizer(tapGesture)
            
        return cell
    }
    
    @objc func handleTap(sender: UITapGestureRecognizer) {
        textField.text = (sender.view as! AutocompleteCell).autocompleteText
        autoCompleteData = []
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

fileprivate class AutocompleteCell: UITableViewCell {
    
    var autocompleteText: String = "" {
        didSet {
            autocompleteLabel.text = autocompleteText
            autocompleteLabel.sizeToFit()
        }
    }
    
    private let autocompleteLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.boldSystemFont(ofSize: 18)
        label.textColor = Colors.tableText
        label.translatesAutoresizingMaskIntoConstraints = false
        
        return label
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        self.contentView.addSubview(autocompleteLabel)
        
        NSLayoutConstraint.activate([
            self.contentView.heightAnchor.constraint(equalTo: autocompleteLabel.heightAnchor, constant: 22),
            
            autocompleteLabel.trailingAnchor.constraint(equalTo: self.contentView.trailingAnchor),
            autocompleteLabel.leadingAnchor.constraint(equalTo: self.contentView.leadingAnchor, constant: 10),
            autocompleteLabel.centerYAnchor.constraint(equalTo: self.contentView.centerYAnchor),
        ])
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

