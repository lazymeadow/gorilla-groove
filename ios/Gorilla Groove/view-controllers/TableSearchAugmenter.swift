import UIKit
import Foundation

class TableSearchAugmenter {
    static func addSearchToNavigation(
        controller: UIViewController,
        tableView: UITableView,
        _ textChanged: @escaping (_ text: String) -> Void
    ) {
        let searchController = UISearchController()
        let searchDelegate = SearchControllerDelegate(controller, tableView, searchController, textChanged)

        let config = UIImage.SymbolConfiguration(pointSize: UIFont.systemFontSize * 1.2, weight: .medium, scale: .large)
        let searchIcon = UIImage(systemName: "magnifyingglass", withConfiguration: config)!
        controller.navigationItem.rightBarButtonItem = UIBarButtonItem(
               image: searchIcon,
               style: .plain,
               action: { searchDelegate.search() }
        )
        
        searchController.obscuresBackgroundDuringPresentation = false
        controller.navigationItem.hidesSearchBarWhenScrolling = false
        
        searchController.searchResultsUpdater = searchDelegate
        searchController.searchBar.delegate = searchDelegate
    }

    
    private class SearchControllerDelegate: NSObject, UISearchResultsUpdating, UISearchBarDelegate {
        var controller: UIViewController
        var tableView: UITableView
        var searchController: UISearchController
        var textChanged: (_ text: String) -> Void

        var handlingSearchEnd = false
        var persistentSearchTerm = ""

        init(
            _ controller: UIViewController,
            _ tableView: UITableView,
            _ searchController: UISearchController,
            _ textChanged: @escaping (_ text: String) -> Void
        ) {
            self.controller = controller
            self.tableView = tableView
            self.searchController = searchController
            self.textChanged = textChanged
        }

        func updateSearchResults(for searchController: UISearchController) {
            let searchTerm = searchController.searchBar.text!

            textChanged(searchTerm)

            
            tableView.reloadData()
        }

        func searchBarTextDidEndEditing(_ searchBar: UISearchBar) {
            // This is triggered when isActive is set to false. We call this method manually sometimes, and set
            // isActive to false at that time. This will cause a re-trigger that we want to ignore.
            // Unfortunately we can't just listen for "searchController.isActive" because that is set false too late
            if (handlingSearchEnd) {
                return
            }
            
            // When you mark a search bar as inactive the term is unhelpfully cleared out. Put it back
            persistentSearchTerm = searchBar.text!
            
            // We want to ignore the re-trigger that iOS does for ending the event when we mark it as inactive manually.
            // Keep around a boolean so we know to ignore it.
            handlingSearchEnd = true
            controller.navigationItem.searchController!.isActive = false
            handlingSearchEnd = false
            
            searchBar.text = persistentSearchTerm
        }
        
        func searchBarCancelButtonClicked(_ searchBar: UISearchBar) {
            controller.navigationItem.searchController!.isActive = false
            controller.navigationItem.searchController = nil
        }
        
        func search() {
             if (controller.navigationItem.searchController == nil) {
                 controller.navigationItem.searchController = searchController
             } else {
                 controller.navigationItem.searchController = nil
             }
             
             let bar = controller.navigationController?.navigationBar
             bar!.sizeToFit()
         }
    }
}
