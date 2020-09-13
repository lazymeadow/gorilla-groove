import Foundation
import UIKit

class SyncController: UIViewController {
    
    private var librarySection: SyncSection? = nil
    private var playlistSection: SyncSection? = nil
    private var userSection: SyncSection? = nil
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let content = UIStackView()
        content.translatesAutoresizingMaskIntoConstraints = false
        content.axis = .vertical
        content.alignment = .center
        content.distribution = .equalSpacing
        
        librarySection = createSection(content, "music.house.fill", "Library")
        playlistSection = createSection(content, "music.note.list", "Playlists")
        userSection = createSection(content, "person.3.fill", "Users")
        
        self.view.addSubview(content)
        self.view.backgroundColor = .white
        
        NSLayoutConstraint.activate([
            content.topAnchor.constraint(equalTo: self.view.topAnchor, constant: 170),
            content.leftAnchor.constraint(equalTo: self.view.leftAnchor),
            content.rightAnchor.constraint(equalTo: self.view.rightAnchor),
            content.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -170),
        ])
    }
    
    override func viewDidAppear(_ animated: Bool) {
        DispatchQueue.global().async {
            self.sync()
            
            // Wait a moment after we finish so people can enjoy / notice everything at 100%
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
//                let libraryView = RootNavigationController()
//                libraryView.modalPresentationStyle = .fullScreen
//                libraryView.modalTransitionStyle = .crossDissolve
//                self.present(libraryView, animated: true, completion: nil)
                
                // This isn't ideal because it isn't animated, but trying to present() or show() the view
                // was resulting in the view either not showing up, or having the incorrect size...
                let appDelegate = UIApplication.shared.delegate as! AppDelegate
                appDelegate.window!.rootViewController = RootNavigationController()
            }
        }
    }
    
    private func createSection(_ content: UIStackView, _ symbol: String, _ sectionName: String) -> SyncSection {
        let section = UIStackView()
        section.translatesAutoresizingMaskIntoConstraints = false
        section.axis = .vertical
        section.alignment = .leading
        section.distribution = .fill
        
        
        // -- Top Row
        
        let topRow = UIStackView()
        topRow.translatesAutoresizingMaskIntoConstraints = false
        topRow.axis = .horizontal
        topRow.alignment = .center
        topRow.distribution = .equalCentering
        
        let iconWrapper = UIStackView()
        iconWrapper.translatesAutoresizingMaskIntoConstraints = false
        iconWrapper.axis = .vertical
        iconWrapper.distribution = .equalCentering
        let labelWrapper = UIStackView()
        labelWrapper.translatesAutoresizingMaskIntoConstraints = false
        labelWrapper.axis = .vertical
        
        let sectionLabel = UILabel()
        sectionLabel.text = sectionName
        sectionLabel.font = UIFont.systemFont(ofSize: 24)
        sectionLabel.sizeToFit()
        
        let iconView = createIcon(symbol)
        iconWrapper.addArrangedSubview(iconView)
        labelWrapper.addArrangedSubview(sectionLabel)
        
        topRow.addArrangedSubview(iconWrapper)
        topRow.addArrangedSubview(labelWrapper)
        
        topRow.setCustomSpacing(20, after: iconWrapper)
        
        
        // -- Bottom Row
        
        let bottomRow = UIStackView()
        bottomRow.translatesAutoresizingMaskIntoConstraints = false
        bottomRow.axis = .horizontal
        
        let progressBar = UISlider()
        progressBar.setThumbImage(UIImage(), for: .normal)
        progressBar.minimumTrackTintColor = Colors.primary
        progressBar.maximumTrackTintColor = Colors.grey4
        progressBar.maximumValue = 1
        progressBar.minimumValue = 0
        progressBar.setValue(0, animated: false)
        
        let percentDone = UILabel()
        percentDone.text = "0%"
        
        bottomRow.addArrangedSubview(progressBar)
        bottomRow.addArrangedSubview(percentDone)
        
        bottomRow.setCustomSpacing(10, after: progressBar)
        
        
        // -- Combine them all
        
        section.addArrangedSubview(topRow)
        section.addArrangedSubview(bottomRow)
        content.addArrangedSubview(section)
        
        section.widthAnchor.constraint(equalTo: content.widthAnchor, constant: -50).isActive = true
        topRow.widthAnchor.constraint(equalTo: section.widthAnchor, multiplier: 0.8).isActive = true
        bottomRow.widthAnchor.constraint(equalTo: section.widthAnchor).isActive = true
        bottomRow.heightAnchor.constraint(equalToConstant: 30).isActive = true
        labelWrapper.widthAnchor.constraint(equalToConstant: 150).isActive = true
        
        return SyncSection(icon: iconView, progressBar: progressBar, progressText: percentDone)
    }
    
    private func createIcon(_ name: String) -> UIImageView {
        let config = UIImage.SymbolConfiguration(pointSize: UIFont.systemFontSize * 2.5, weight: .medium, scale: .large)
        
        let icon = UIImageView(image: UIImage(systemName: name, withConfiguration: config)!)
        icon.tintColor = .black
        
        return icon
    }
    
    private func sync() {
        ServerSynchronizer.syncWithServer() { completedPage, totalPages, type in
            DispatchQueue.main.async {
                let percentDone: Float = {
                    if (totalPages == 1) {
                        return 1.0 // Avoid division by zero...
                    } else {
                        return Float(completedPage) / Float(totalPages - 1)
                    }
                }()
                
                let syncSection: SyncSection = try! {
                    if (type == "track") {
                        return self.librarySection!
                    } else if (type == "playlist") {
                        return self.playlistSection!
                    } else if (type == "user") {
                        return self.userSection!
                    } else {
                        throw "Unimplemented entity type was synced!"
                    }
                }()
                
                syncSection.progressBar.setValue(percentDone, animated: true)
                
                let percentDoneText = String(Int(round(percentDone * 100))) + "%"
                syncSection.progressText.text = percentDoneText
                
                if (completedPage == totalPages - 1) {
                    syncSection.progressText.textColor = Colors.primary
                    syncSection.icon.tintColor = Colors.primary
                }
            }
        }
    }
    
    struct SyncSection {
        let icon: UIImageView
        let progressBar: UISlider
        let progressText: UILabel
    }
}

extension UIStackView {
    func addBackground(color: UIColor) {
        let subView = UIView(frame: bounds)
        subView.backgroundColor = color
        subView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        insertSubview(subView, at: 0)
    }
}

extension String: Error {}
