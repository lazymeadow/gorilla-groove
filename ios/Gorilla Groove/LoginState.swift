//
//  LoginState.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/5/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import Foundation

class LoginState {
    static func save(_ loginResponse: LoginResponse) {
        let encoder = PropertyListEncoder()
        encoder.outputFormat = .xml
        
        let path = getPlistPath()
        
        do {
            let data = try encoder.encode(loginResponse)
            try data.write(to: URL(fileURLWithPath: path))
        } catch {
            print(error)
        }
    }
    
    static func read() -> LoginResponse? {
        let path = getPlistPath()
        
        let xml = FileManager.default.contents(atPath: path)
        if (xml == nil) {
            return nil
        }
        let savedState = try? PropertyListDecoder().decode(LoginResponse.self, from: xml!)
        
        return savedState
    }
    
    static func clear() {
        let path = getPlistPath()
        
        try? FileManager.default.removeItem(atPath: path)
    }
    
    static private func getPlistPath() -> String {
        let plistFileName = "data.plist"
        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        let documentPath = paths[0] as NSString
        let plistPath = documentPath.appendingPathComponent(plistFileName)
        return plistPath
    }
}
