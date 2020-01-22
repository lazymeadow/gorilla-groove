import Foundation

class HttpRequester {
    
    static let baseUrl = "https://gorillagroove.net/api/"
    
    static func get<T: Codable>(
        _ url: String,
        _ type: T.Type,
        callback: @escaping (_ data: T?, _ status: Int, _ err: String?) -> Void
    ) {
        let session = URLSession(configuration: .default)
        let request = getBaseRequest("GET", url)
        
        print("Network - GET " + request.url!.absoluteString)
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func put<T: Codable>(
        _ url: String,
        _ type: T.Type,
        _ body: Codable?,
        callback: @escaping (_ data: T?, _ status: Int, _ err: String?) -> Void
    ) {
        let session = URLSession(configuration: .default)
        let request = getBaseRequest("PUT", url, body: body)
        
        print("Network - POST " + request.url!.absoluteString)
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func post<T: Codable>(
        _ url: String,
        _ type: T.Type,
        _ body: Codable?,
        callback: @escaping (_ data: T?, _ status: Int, _ err: String?) -> Void
    ) {
        let session = URLSession(configuration: .default)
        let request = getBaseRequest("POST", url, body: body)
        
        print("Network - PUT " + request.url!.absoluteString)
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static private func getBaseRequest(_ method: String, _ url: String, body: Codable? = nil) -> URLRequest {
        let token = LoginState.read()!.token
        
        let url = URL(string: self.baseUrl + url)!
        var request : URLRequest = URLRequest(url: url)
        
        request.httpMethod = method
        request.setValue(token, forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if (body != nil) {
            request.httpBody = body?.toJSONData()
        }
        
        return request
    }
    
    static private func handleResponse<T: Codable>(
        _ data: Data?,
        _ type: T.Type,
        _ response: URLResponse?,
        _ error: Error?,
        _ callback: @escaping (_ data: T?, _ status: Int, _ err: String?
    ) -> Void) {
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
}

extension Encodable {
    func toJSONData() -> Data? {
        return try! JSONEncoder().encode(self)
    }
}

struct EmptyResponse: Codable { }
