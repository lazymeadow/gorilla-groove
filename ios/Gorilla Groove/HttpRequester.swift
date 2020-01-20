//
//  HttpRequester.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/11/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import Foundation

class HttpRequester {
    
    static let baseUrl = "https://gorillagroove.net/api/"
    
    static func get<T: Codable>(
        _ url: String,
        _ type: T.Type,
        callback: @escaping (_ data: T?, _ status: Int, _ err: String?) -> Void
    ) {
        let token = LoginState.read()!.token
        
        let session = URLSession(configuration: .default)
        let url = URL(string: self.baseUrl + url)!
        var request : URLRequest = URLRequest(url: url)
        
        request.httpMethod = "GET"
        request.setValue(token, forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        print("Network - GET " + url.absoluteString)
        let dataTask = session.dataTask(with: request) { data, response, error in
            guard let httpResponse = response as? HTTPURLResponse
                else {
                    print("error: not a valid http response")
                    return
            }
            
            if (httpResponse.statusCode >= 300) {
                print("Non 2xx received! Code: \(httpResponse.statusCode)")
                callback(nil, httpResponse.statusCode, error as! String?)
                return
            }
            
            let decoder = JSONDecoder()
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
            decoder.dateDecodingStrategy = .formatted(dateFormatter)
            
            let decodedData = try! decoder.decode(T.self, from: data!)
            
            callback(decodedData, httpResponse.statusCode, error as! String?)
        }
        dataTask.resume()
    }
    
    static func put<T: Codable>(
        _ url: String,
        _ type: T.Type,
        _ body: Codable?,
        callback: @escaping (_ data: T?, _ status: Int, _ err: String?) -> Void
    ) {
        let token = LoginState.read()!.token
        
        let session = URLSession(configuration: .default)
        let url = URL(string: self.baseUrl + url)!
        var request : URLRequest = URLRequest(url: url)
        
        request.httpMethod = "PUT"
        request.setValue(token, forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        if (body != nil) {
            print("Trying to serialize")
            request.httpBody = body?.toJSONData()
        }

        print("Network - POST " + url.absoluteString)
        let dataTask = session.dataTask(with: request) { data, response, error in
            guard let httpResponse = response as? HTTPURLResponse
                else {
                    print("error: not a valid http response")
                    return
            }
            
            if (httpResponse.statusCode >= 300) {
                print("Non 2xx received! Code: \(httpResponse.statusCode)")
                callback(nil, httpResponse.statusCode, error as! String?)
                return
            }
            
            // We aren't expecting the server to give us anything, so don't bother decoding
            print(type)
            if (type == EmptyResponse.self) {
                return callback(nil, httpResponse.statusCode, error as! String?)
            }
            
            let decoder = JSONDecoder()
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
            decoder.dateDecodingStrategy = .formatted(dateFormatter)
            
            let decodedData = try! decoder.decode(T.self, from: data!)
            
            callback(decodedData, httpResponse.statusCode, error as! String?)
        }
        dataTask.resume()
    }
}

extension Encodable {
    func toJSONData() -> Data? {
        return try! JSONEncoder().encode(self)
    }
}

struct EmptyResponse: Codable { }
